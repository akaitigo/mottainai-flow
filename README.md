# mottainai-flow

余剰食品の最適配車プラットフォーム。スーパー・農家・食品メーカーの余剰在庫をフードバンク・子ども食堂へ最適ルートで配送する。

## Quick Start

### 前提条件

- Docker & Docker Compose
- (開発用) Java 21, Go 1.24+, Node.js 22+, buf CLI

### Docker Compose で起動

```bash
cp .env.example .env
docker compose up -d
```

サービス一覧:

| サービス | ポート | 説明 |
|----------|--------|------|
| postgres | 5432 | PostgreSQL + PostGIS |
| backend | 9090 | Kotlin/Quarkus gRPC API |
| routing | 9091 | Go ルート最適化サービス |
| frontend | 3000 | React フロントエンド |

### ローカル開発

```bash
# 全チェック (format + lint + test + build)
make check

# 個別コマンド
make build    # 全サービスビルド
make test     # 全テスト実行
make lint     # 全 lint 実行
make format   # 全フォーマット実行
```

### Proto 定義

```bash
buf lint          # lint チェック
buf format -w     # フォーマット
buf generate      # Go コード生成
```

## アーキテクチャ

```
backend/   — Kotlin + Quarkus (gRPC API)
routing/   — Go (ルート最適化 VRP ソルバー)
frontend/  — TypeScript + React
proto/     — gRPC 定義 (buf 管理)
db/        — PostgreSQL + PostGIS 初期化スクリプト
docs/adr/  — Architecture Decision Records
```

## ADR

- [ADR-001](docs/adr/ADR-001-inventory-data-model.md): 在庫データモデル設計
- [ADR-002](docs/adr/ADR-002-or-tools-go-binding.md): OR-Tools Go バインディング選定
