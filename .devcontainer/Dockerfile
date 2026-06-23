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

# ----------------------------------------------------------------------
# JDK（Android Gradle Plugin 8.x が要求する JDK 17 / Debian bookworm = OpenJDK 17）
# default-jdk は /usr/lib/jvm/default-java シンボリックリンクを作るためアーキ非依存で JAVA_HOME を設定できる。
# ----------------------------------------------------------------------
RUN apt-get update && apt-get install -y default-jdk \
    && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/usr/lib/jvm/default-java

# ----------------------------------------------------------------------
# Android SDK（コマンドラインツール + platform-tools + platform + build-tools）
# コンテナ内で .aab/.apk のネイティブビルドを行うために導入する。
# ----------------------------------------------------------------------
ARG ANDROID_CMDLINE_TOOLS_VERSION=14742923
ARG ANDROID_API_LEVEL=35
ARG ANDROID_BUILD_TOOLS=35.0.0
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="${PATH}:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools"
RUN set -eux; \
    mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools"; \
    curl --proto '=https' --tlsv1.2 -fsSL \
      "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMDLINE_TOOLS_VERSION}_latest.zip" \
      -o /tmp/cmdline-tools.zip; \
    unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools-extract; \
    mv /tmp/cmdline-tools-extract/cmdline-tools "${ANDROID_SDK_ROOT}/cmdline-tools/latest"; \
    rm -rf /tmp/cmdline-tools.zip /tmp/cmdline-tools-extract; \
    yes | sdkmanager --licenses > /dev/null; \
    sdkmanager \
      "platform-tools" \
      "platforms;android-${ANDROID_API_LEVEL}" \
      "build-tools;${ANDROID_BUILD_TOOLS}"; \
    chown -R node:node "${ANDROID_SDK_ROOT}"

# ----------------------------------------------------------------------
# Gradle（プロジェクトに gradlew が無くてもブートストラップできるよう同梱）
# 既存プロジェクトでは通常 ./gradlew（Gradle Wrapper）を利用する。
# ----------------------------------------------------------------------
ARG GRADLE_VERSION=8.13
RUN set -eux; \
    curl --proto '=https' --tlsv1.2 -fsSL \
      "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
      -o /tmp/gradle.zip; \
    unzip -q /tmp/gradle.zip -d /opt; \
    ln -s "/opt/gradle-${GRADLE_VERSION}/bin/gradle" /usr/local/bin/gradle; \
    rm -f /tmp/gradle.zip

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

# Android SDK のユーザ設定ディレクトリ（.android）を node 所有で用意
RUN mkdir -p /home/node/.android \
    && chown -R node:node /home/node/.android

# 作業ディレクトリ
WORKDIR /workspace

USER node
