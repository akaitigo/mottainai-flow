# mottainai-flow
<!-- 余剰食品の最適配車プラットフォーム -->

## コマンド
- ビルド: `make build`
- テスト: `make test`
- lint: `make lint`
- フォーマット: `make format`
- 全チェック: `make check`

## ワークフロー
1. research.md を作成（調査結果の記録）
2. plan.md を作成（実装計画。人間承認まで実装禁止）
3. 承認後に実装開始。plan.md のtodoを進捗管理に使用

## ルール
- ADR: docs/adr/ 参照。新規決定はADRを書いてから実装
- テスト: 機能追加時は必ずテストを同時に書く
- lint設定の変更禁止（ADR必須）

## 構造
- backend/ — Kotlin + Quarkus (gRPC API)
- routing/ — Go + OR-Tools (ルート最適化)
- frontend/ — TypeScript + React
- proto/ — gRPC定義 (buf管理)

## 禁止事項
- any型(TS) / !!(Kotlin) / 未検証キャスト
- console.log / print文のコミット
- TODO コメントのコミット（Issue化すること）
- .env・credentials のコミット
- lint設定の無効化

## Hooks
- 設定: .claude/hooks/ 参照

## 状態管理
- git log + GitHub Issues でセッション間の状態を管理
- セッション開始: `bash startup.sh`
