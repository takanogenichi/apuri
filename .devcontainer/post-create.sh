#!/bin/bash
set -e

echo "=== postCreateCommand 開始 ==="

# git 設定
git config --global --add safe.directory /workspace

# pnpm install（package.json が存在する場合）
if [ -f /workspace/package.json ]; then
  cd /workspace
  pnpm install
  echo "pnpm install 完了"
else
  echo "package.json が無いため pnpm install はスキップ"
fi

# zsh の設定（PATH と kiro 起動関数）
if [ -f ~/.zshrc ]; then
  if ! grep -q "node_modules/.bin" ~/.zshrc; then
    echo 'export PATH="/workspace/node_modules/.bin:$PATH"' >> ~/.zshrc
  fi
  if ! grep -q "kiro()" ~/.zshrc; then
    cat >> ~/.zshrc <<'EOF'

# kiro-cli 起動（未ログインなら自動で SSO ログイン / trust-all）
KIRO_IDP="https://d-95674b0b93.awsapps.com/start"
KIRO_REGION="ap-northeast-1"
kiro() {
  kiro-cli whoami >/dev/null 2>&1 || \
    kiro-cli login --license pro --identity-provider "$KIRO_IDP" --region "$KIRO_REGION" --use-device-flow
  kiro-cli chat --trust-all-tools
}
EOF
  fi
fi

echo "=== postCreateCommand 完了 ==="
