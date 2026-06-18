FROM node:20-bookworm

# システムパッケージのインストール
RUN apt-get update && apt-get install -y \
    git \
    curl \
    wget \
    unzip \
    zip \
    vim \
    zsh \
    jq \
    sudo \
    && rm -rf /var/lib/apt/lists/*

# pnpm のインストール
RUN corepack enable && corepack prepare pnpm@latest --activate

# AWS Kiro CLI をインストール。
# node:20-bookworm は Debian 12 (glibc 2.36) のため、glibc 2.34+ を要求する標準ビルドの ZIP を利用する。
# install.sh は root 実行時 /root/.local/bin にバイナリを置くが、本 devContainer の実行ユーザは
# node であり /root(700) を辿れない。そのため全ユーザ実行可能な /usr/local/bin へ移動して PATH を通す。
ARG KIRO_CLI_VERSION=latest
RUN set -eux; \
    arch="$(dpkg --print-architecture)"; \
    case "$arch" in \
      amd64) kiroArch="x86_64" ;; \
      arm64) kiroArch="aarch64" ;; \
      *) echo "unsupported arch: $arch" >&2; exit 1 ;; \
    esac; \
    curl --proto '=https' --tlsv1.2 -fsSL \
      "https://desktop-release.q.us-east-1.amazonaws.com/${KIRO_CLI_VERSION}/kirocli-${kiroArch}-linux.zip" \
      -o /tmp/kirocli.zip; \
    unzip -q /tmp/kirocli.zip -d /tmp; \
    /tmp/kirocli/install.sh --no-confirm --force; \
    mv /root/.local/bin/kiro-cli /root/.local/bin/kiro-cli-chat /root/.local/bin/kiro-cli-term /usr/local/bin/; \
    chmod 755 /usr/local/bin/kiro-cli /usr/local/bin/kiro-cli-chat /usr/local/bin/kiro-cli-term; \
    rm -rf /tmp/kirocli /tmp/kirocli.zip

# oh-my-zsh のインストール（node ユーザ用）
USER node
RUN sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)" "" --unattended
USER root

# node ユーザに sudo 権限を付与
RUN echo "node ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

# kiro-cli の認証/データ永続化ディレクトリ（docker-compose の kirocli-data ボリュームにマウント）
RUN mkdir -p /home/node/.local/share/kiro-cli \
    && chown -R node:node /home/node/.local

# 作業ディレクトリ
WORKDIR /workspace

USER node
