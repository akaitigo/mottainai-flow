# Harvest Report: mottainai-flow

> 収穫日: 2026-03-30
> リポジトリ: akaitigo/mottainai-flow
> 概要: 余剰食品の最適配車プラットフォーム

## プロジェクト概要

mottainai-flow は余剰食品のフードロスを削減するため、提供者と受取者のマッチングおよび集荷ルート最適化を行うプラットフォーム。Kotlin/Quarkus (gRPC API)、Go (VRP ルート最適化)、TypeScript/React (フロントエンド) の多言語構成で、Protocol Buffers (buf) によるスキーマ駆動開発を採用。

**技術スタック:**
- Backend: Kotlin + Quarkus + gRPC
- Routing: Go + OR-Tools (VRP ソルバー)
- Frontend: TypeScript + React + Vite
- Schema: Protocol Buffers (buf)
- DB: PostgreSQL (Docker Compose)

## メトリクス

| メトリクス | 値 |
|---|---|
| Issue 数 | 5 (全 CLOSED) |
| PR 数 | 5 (全 MERGED) |
| Issue 消化率 | 100% (5/5) |
| PR マージ率 | 100% (5/5) |
| コミット数 (non-merge) | 7 |
| ADR 数 | 2 |
| テスト数 | 89 (Kotlin: 76, Go: 12, TS: 1) |
| テスト結果 | 全 PASS |
| CLAUDE.md 行数 | 39 (上限 50 以内) |
| settings.json | あり |
| startup.sh | なし |
| lefthook.yml | あり |
| Docker Compose | あり |
| GitHub Actions CI | なし |
| LICENSE | あり |
| PRD.md | あり |
| README.md | あり |

## ハーネス適用状況

### Layer-0: Foundation (プロジェクト基盤)

| 項目 | 適用 | 備考 |
|---|---|---|
| CLAUDE.md | YES | 39行、構造化済み。コマンド・ワークフロー・ルール・禁止事項を網羅 |
| Makefile | YES | build / test / lint / format / check / quality / clean 完備 |
| LICENSE | YES | MIT |
| .gitignore | YES | - |
| README.md | YES | - |

### Layer-1: Guard Rails (品質ガード)

| 項目 | 適用 | 備考 |
|---|---|---|
| settings.json (hooks) | YES | PreToolUse (破壊コマンド・秘密ファイル・設定変更ブロック)、PostToolUse (自動lint)、PreCompact (CLAUDE.mdバックアップ)、Stop (make check + quality) |
| lefthook.yml | YES | pre-commit: lint / format / test / archgate (並列実行) |
| Linter (Kotlin) | YES | detekt |
| Linter (Go) | YES | golangci-lint |
| Linter (TS) | YES | oxlint + biome |
| Formatter (Kotlin) | YES | ktlint (formatKotlin) |
| Formatter (Go) | YES | gofumpt + goimports |
| Formatter (TS) | YES | biome format |
| Proto lint/format | YES | buf lint / buf format |
| post-lint.sh | YES | PostToolUse hook で自動実行 |
| quality gate | YES | make quality: LICENSE、TODO/FIXME、ハードコード秘密、PRD チェック、CLAUDE.md 行数制限 |

### Layer-2: Process (開発プロセス)

| 項目 | 適用 | 備考 |
|---|---|---|
| ADR | YES | 2件 (inventory-data-model, or-tools-go-binding) |
| PRD | YES | PRD.md 存在 |
| Issue ラベル運用 | YES | mvp / model:haiku,sonnet,opus / status:backlog |
| GitHub Actions CI | NO | 未設定。lefthook によるローカルガードのみ |
| startup.sh | NO | CLAUDE.md に記載あるが、ファイル未作成 |
| Docker Compose | YES | DB初期化含むインフラ構成 |

### 適用サマリ

- **Layer-0:** 5/5 (100%)
- **Layer-1:** 11/11 (100%)
- **Layer-2:** 4/6 (67%)
- **総合:** 20/22 (91%)

## テンプレート改善提案

| # | カテゴリ | 提案 | 根拠 | 優先度 |
|---|---|---|---|---|
| 1 | テンプレート | GitHub Actions CI ワークフローを初期テンプレートに追加 | lefthook のみではリモート保護が不十分。PR マージ前にCIで品質ゲートを強制すべき | HIGH |
| 2 | テンプレート | startup.sh を CLAUDE.md と同時に生成 | CLAUDE.md に `bash startup.sh` の記載があるが実体なし。テンプレート生成時に一貫して作成すべき | HIGH |
| 3 | テンプレート | フロントエンドテストの最低数ガードを追加 | TS テスト 1件のみ。quality gate にテストカバレッジ閾値チェックを追加 | MEDIUM |
| 4 | プロセス | ADR テンプレートに「代替案の比較表」セクションを標準化 | 意思決定の透明性向上。振り返り時に「なぜこの選択か」を追跡しやすくする | MEDIUM |
| 5 | ハーネス | make quality に `test -d .github/workflows` チェックを追加 | CI 未設定をship前に検出できる | LOW |
| 6 | ハーネス | CLAUDE.md のワークフローに「振り返り → harvest.md」ステップを追加 | プロジェクト完了時の振り返りが定型化されていない。テンプレートに組み込むことで習慣化 | LOW |

## 振り返り

### 良かった点

1. **Issue 消化率・PR マージ率ともに 100%** — 計画したMVPスコープを完全に消化。5 Issue / 5 PR が全てクローズ・マージされており、スコープクリープなし
2. **ハーネス適用率 91%** — Layer-0 / Layer-1 は完全適用。settings.json の4種フック (PreToolUse, PostToolUse, PreCompact, Stop) による多層防御が効いている
3. **多言語プロジェクトでの品質統一** — Kotlin (detekt + ktlint)、Go (golangci-lint + gofumpt)、TS (oxlint + biome)、Proto (buf lint/format) と全言語でlint/formatが整備
4. **ADR 駆動の意思決定** — データモデル設計とOR-Toolsバインディング選択をADRで記録。lint設定変更をADR必須にする settings.json フックも実装
5. **コンパクトなコミット履歴** — 7コミットでMVP完成。各PRが機能単位で明確に分離

### 改善点

1. **GitHub Actions CI が未設定** — lefthook によるローカルガードは完璧だが、リモートでの品質ゲートがない。PR保護ルールと組み合わせたCI必須化が必要
2. **startup.sh が未作成** — CLAUDE.md で参照しているが実体がない。セッション開始時のコンテキスト復元が手動になっている
3. **フロントエンドテストが薄い** — Kotlin 76件、Go 12件に対し、TS は 1件のみ。UIコンポーネントのテスト戦略が未定義
4. **v1.0.0 タグ後のCHANGELOG未作成** — リリースタグは切られているがCHANGELOGが存在しない
5. **Integration Test 環境の未整備** — Docker Compose はあるがE2Eテスト（gRPC クライアント → Backend → Routing の結合テスト）が未実装
