#!/usr/bin/env bash
# =============================================================================
# セッション起動ルーチン
#
# セッション開始時に実行し、ツールの自動インストールとヘルスチェックを行う。
# 状態管理は git log + GitHub Issues で行う。
#
# オプション:
#   --dev    開発サーバーも起動する（Web App/API 向け）
#   --skip-checks  ヘルスチェックをスキップ（デバッグ用）
# =============================================================================
set -euo pipefail

START_DEV=false
SKIP_CHECKS=false

for arg in "$@"; do
  case "$arg" in
    --dev) START_DEV=true ;;
    --skip-checks) SKIP_CHECKS=true ;;
  esac
done

echo "=== Session Startup ==="

# 1. 作業ディレクトリ確認
[ -d ".git" ] || { echo "ERROR: Not in git repository"; exit 1; }

# 1.5. 言語検出と必須ツールの自動インストール
echo "=== Tool auto-install ==="

# Kotlin/JVM (backend)
if [ -f "backend/build.gradle.kts" ] || [ -f "backend/build.gradle" ]; then
  echo "Detected: Kotlin/JVM (backend/)"
  if [ -f "backend/gradlew" ]; then
    chmod +x backend/gradlew
  fi
fi

# Go (routing)
if [ -f "routing/go.mod" ]; then
  echo "Detected: Go (routing/)"
  command -v golangci-lint &>/dev/null || { echo "Installing golangci-lint..."; go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest 2>/dev/null || echo "WARN: golangci-lint install failed"; }
  command -v gofumpt &>/dev/null || { echo "Installing gofumpt..."; go install mvdan.cc/gofumpt@latest 2>/dev/null || echo "WARN: gofumpt install failed"; }
fi

# TypeScript/React (frontend)
if [ -f "frontend/package.json" ]; then
  echo "Detected: TypeScript/React (frontend/)"
  command -v oxlint &>/dev/null || { echo "Installing oxlint..."; npm install -g oxlint 2>/dev/null || echo "WARN: oxlint install failed"; }
fi

# buf (gRPC)
if [ -f "buf.yaml" ]; then
  echo "Detected: gRPC (buf.yaml)"
  command -v buf &>/dev/null || { echo "WARN: buf not installed. See https://buf.build/docs/installation"; }
fi

# lefthook
command -v lefthook &>/dev/null || { echo "Installing lefthook..."; go install github.com/evilmartians/lefthook@latest 2>/dev/null || npm install -g lefthook 2>/dev/null || echo "WARN: lefthook install failed"; }
if command -v lefthook &>/dev/null && [ -f "lefthook.yml" ]; then
  lefthook install 2>/dev/null && echo "lefthook hooks installed." || echo "WARN: lefthook install failed"
fi
echo "Tool check complete."

# 2. Gitログ読取
echo "=== Recent commits ==="
git log --oneline -10

# 3. ヘルスチェック
if [ "$SKIP_CHECKS" = true ]; then
  echo "=== Health check SKIPPED (--skip-checks) ==="
else
  echo "=== Health check ==="
  if make check 2>&1 | tail -10; then
    echo "All checks passed. Ready to work."
  else
    echo "WARN: Checks failed. Review issues before proceeding."
  fi
fi

# 4. 開発サーバー起動（オプション）
if [ "$START_DEV" = true ]; then
  echo "=== Starting dev server ==="
  echo "WARN: Dev server startup not yet configured. Add to Makefile."
fi

echo ""
echo "=== Session started at $(date -u +"%Y-%m-%dT%H:%M:%SZ") ==="
echo "Ready to work. State management: git log + GitHub Issues."
