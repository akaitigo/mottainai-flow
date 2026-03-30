# Changelog

## v1.0.0 (2026-03-30)

MVP release — 余剰食品の最適配車プラットフォーム。

### Features
- **Docker Compose infrastructure**: DB初期化スクリプト、Dockerfile、README (#10)
- **F3: 受領確認とトレーサビリティ管理**: 配送者の受領記録、ステータス追跡、レポート生成 (#9)
- **F1-F2: 需給マッチングエンジン**: 距離・時間帯・品目による自動マッチング (#8)
- **F2: ルート最適化サービス**: Go + VRP ソルバーによる最適集荷ルート計算 (#7)
- **F1: 在庫登録API**: Kotlin/Quarkus + gRPC による提供・受取在庫登録 (#6)

### Infrastructure
- 初期プロジェクトセットアップ (harness templates)
