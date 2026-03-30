#!/usr/bin/env bash
# PostToolUse hook: ファイル編集後の自動lint
# エラーは警告として表示（ブロックしない）
set -uo pipefail

INPUT=$(cat)
FILE=$(echo "$INPUT" | jq -r '.file_path // empty')
if [ -z "$FILE" ]; then
  exit 0
fi

EXT="${FILE##*.}"
case "$EXT" in
  kt|kts)
    echo "INFO: Kotlin file changed. Run 'make lint' to check."
    ;;
  go)
    echo "INFO: Go file changed. Run 'make lint' to check."
    ;;
  ts|tsx)
    echo "INFO: TypeScript file changed. Run 'make lint' to check."
    ;;
  proto)
    if command -v buf &>/dev/null; then
      buf format -w 2>/dev/null || true
      buf lint 2>&1 || echo "WARN: buf lint found issues"
    fi
    ;;
esac
