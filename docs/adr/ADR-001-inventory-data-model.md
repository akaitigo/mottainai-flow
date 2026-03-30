# ADR-001: F1 在庫データモデル設計

**日付**: 2026-03-30
**ステータス**: 承認済み
**決定者**: 開発チーム

## コンテキスト

mottainai-flow の中核機能である「提供・受取在庫の登録API」(F1) のデータモデルを設計する必要がある。以下の要件を満たすこと:

- 提供者が余剰食品を登録し、受取者が必要な食品を登録できる
- 住所からジオコーディングした位置情報を保存し、距離ベースのマッチング（F1-F2）に使える
- ページネーション付きの一覧取得が効率的に行える
- 受領記録は3年間保存（非機能要件）

## 決定要因

- F2（マッチングエンジン）で `ST_DWithin` による距離フィルタを行うため、空間インデックスが必須
- Supply と Demand は属性が異なる（消費期限 vs 希望カテゴリ）ため、テーブル設計で差異を吸収する必要がある
- 一覧APIは頻繁に呼ばれるため、ページネーションの効率がUXに直結する
- PostgreSQL + PostGIS は技術スタック確定済み（PRD）

## 検討した選択肢

### 選択肢A: Supply/Demand 単一テーブル（STI: Single Table Inheritance）

**メリット**: テーブル1つで管理がシンプル。JOIN不要。
**デメリット**: NULL カラムが多発（Supply固有の `expiry_date`、Demand固有の `desired_quantity`）。型安全性が低下。カラム追加時に相互影響。

### 選択肢B: Supply/Demand テーブル分離

**メリット**: 各テーブルが自身のドメインに最適化。NULL カラム不要。独立したインデックス設計。スキーマ変更の影響範囲が限定的。
**デメリット**: マッチング時にクロステーブルクエリが必要。マイグレーションファイルが増える。

### 選択肢C: EAV（Entity-Attribute-Value）パターン

**メリット**: 属性追加が柔軟。
**デメリット**: クエリが複雑化。型安全性なし。パフォーマンス劣化。過剰な汎用化。

## 決定

### テーブル設計: 選択肢Bを選択（Supply/Demand テーブル分離）

Supply と Demand はライフサイクルもステータス遷移も異なるため、分離が自然。マッチングクエリのクロステーブルJOINは空間インデックスで十分高速化できる。

### 位置情報: PostGIS GEOGRAPHY 型を採用

GEOMETRY 型（平面座標）ではなく GEOGRAPHY 型（測地系）を選択。理由:
- 日本国内の距離計算で地球の曲率を考慮した正確な結果が得られる
- `ST_DWithin(geog, geog, meters)` でメートル単位の距離フィルタが直感的に書ける
- GEOMETRY + SRID:4326 でも可能だが、距離計算のたびに `ST_Transform` が必要になる

### ページネーション: Keyset 方式を採用

OFFSET方式ではなく Keyset（Cursor）方式を選択。理由:
- OFFSET はページが深くなるほど遅くなる（O(offset+limit)）
- Keyset は常に O(limit) で安定したパフォーマンス
- `created_at` + `id` の複合キーでユニーク性を保証
- gRPC の `page_token` にエンコードして返却

## 結果

**ポジティブ**:
- Supply/Demand が独立してスキーマ進化可能
- GEOGRAPHY 型により距離計算が正確かつ高速
- Keyset ページネーションによりデータ量増加に対してスケーラブル

**ネガティブ/トレードオフ**:
- GEOGRAPHY 型は GEOMETRY 型より一部の空間演算が遅い（ただし `ST_DWithin` は最適化済み）
- Keyset 方式はランダムページジャンプ不可（F1のユースケースでは不要）
- テーブル分離によりマッチングクエリがやや複雑化

**リスク**:
- GEOGRAPHY 型のインデックスサイズが大きくなる可能性（数万件規模では問題なし）

## 実装メモ

```sql
-- supplies テーブル
CREATE TABLE supplies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id VARCHAR(100) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    category SMALLINT NOT NULL DEFAULT 0,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit VARCHAR(20) NOT NULL,
    expiry_date TIMESTAMPTZ NOT NULL,
    pickup_window_start TIMESTAMPTZ NOT NULL,
    pickup_window_end TIMESTAMPTZ NOT NULL,
    postal_code VARCHAR(10),
    prefecture VARCHAR(10),
    city VARCHAR(50),
    street VARCHAR(200),
    location GEOGRAPHY(POINT, 4326),
    status SMALLINT NOT NULL DEFAULT 1,
    description TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (pickup_window_start < pickup_window_end)
);

CREATE INDEX idx_supplies_location ON supplies USING GIST (location);
CREATE INDEX idx_supplies_status_created ON supplies (status, created_at DESC, id);
CREATE INDEX idx_supplies_provider ON supplies (provider_id);

-- demands テーブル
CREATE TABLE demands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id VARCHAR(100) NOT NULL,
    category SMALLINT NOT NULL DEFAULT 0,
    desired_quantity INT NOT NULL CHECK (desired_quantity > 0),
    unit VARCHAR(20) NOT NULL,
    delivery_window_start TIMESTAMPTZ NOT NULL,
    delivery_window_end TIMESTAMPTZ NOT NULL,
    postal_code VARCHAR(10),
    prefecture VARCHAR(10),
    city VARCHAR(50),
    street VARCHAR(200),
    location GEOGRAPHY(POINT, 4326),
    status SMALLINT NOT NULL DEFAULT 1,
    description TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (delivery_window_start < delivery_window_end)
);

CREATE INDEX idx_demands_location ON demands USING GIST (location);
CREATE INDEX idx_demands_status_created ON demands (status, created_at DESC, id);
CREATE INDEX idx_demands_recipient ON demands (recipient_id);
```

- ロールバック: テーブルの DROP で復元可能（初回マイグレーション）
- page_token は `Base64(created_at|id)` でエンコード
