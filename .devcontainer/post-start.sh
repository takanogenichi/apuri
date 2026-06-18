#!/bin/bash
set -e

echo "=== postStartCommand 開始 ==="

# kiro-cli の既定モデルを設定（ログイン状態に依存しない設定のみ）
if command -v kiro-cli >/dev/null 2>&1; then
  kiro-cli settings chat.defaultModel claude-opus-4.8 || true
  echo "kiro-cli 既定モデルを設定しました (claude-opus-4.8)"
fi

echo "=== postStartCommand 完了 ==="
