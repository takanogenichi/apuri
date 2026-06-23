# 実施計画: DevContainer 再構築（kiro 対応 / ポート 40000 台）

## ゴール

- axolLoop と同形式の DevContainer 構成を `apuri` に再構築する。
- app コンテナ（`al`）のみとし、axolLoop 由来の MySQL/Redis/MinIO/Mailpit は**含めない**。
- コンテナ内で `make kiro`（または `kiro`）で kiro-cli を起動できる。
- ポートは 40000 台（インスタンス1で `40001`、`+10` 刻みで 1〜8）。
- `AGENTS.md` のコンテナ表を 40000 台へ更新する。

## 方針メモ（軸の確定）

- **ベースイメージ / 実行ユーザ**: axolLoop に合わせ `node:20-bookworm` / `remoteUser=node` で統一する。
  - node:20-bookworm は glibc 2.36 のため、kiro-cli は glibc 2.34+ 要求の標準ビルド ZIP を使用可能。
  - いただいた kiro スニペットは `vscode` ユーザ / `/home/vscode/...` 前提だが、本構成では `node` ユーザに合わせ `/home/node/.local/share/kiro-cli` に読み替える。
- **役割分担（最終決定: コンテナ内ビルド）**: コンテナ内で「コード編集 + kiro による AI 支援 + Android のネイティブビルド（.aab/.apk）」まで行う。
  - そのため **コンテナに JDK 17 + Android SDK + Gradle を導入する**。
  - **JDK**: Debian `default-jdk`（bookworm = OpenJDK 17）、`JAVA_HOME=/usr/lib/jvm/default-java`。
  - **Android SDK**: cmdline-tools(14742923) + platform-tools + `platforms;android-35` + `build-tools;35.0.0`、ライセンス自動承諾、`ANDROID_SDK_ROOT=/opt/android-sdk`、node 所有。
  - **Gradle**: 8.13 を同梱（wrapper 非保有プロジェクトのブートストラップ用）。既存プロジェクトは `./gradlew` を利用。
  - バージョンはすべて `ARG`（`ANDROID_CMDLINE_TOOLS_VERSION` / `ANDROID_API_LEVEL` / `ANDROID_BUILD_TOOLS` / `GRADLE_VERSION` / `KIRO_CLI_VERSION`）で変更可能。
  - エミュレーター/実機検証はホストの Android Studio で行う（コンテナ内エミュレーターは仮想化支援・GUI 制約のため対象外）。
  - 注意（arm64/Apple Silicon）: コンテナは arm64 で動作する。Android build-tools は近年 aarch64 Linux バイナリを提供しており動作見込みだが、特定バージョンで非対応の場合は `ANDROID_BUILD_TOOLS` を調整する。
- **サービス簡素化**: `al` 単一サービスのため、MinIO バケット初期化・DB 待ち・socat による他コンテナ転送は不要。`post-start.sh` は kiro 設定の適用のみとする。
- **kiro バイナリ取得**: 公式配布 ZIP をビルド時に取得・インストールし、`/usr/local/bin` に配置（全ユーザ実行可）。
  - URL: `https://desktop-release.q.us-east-1.amazonaws.com/${KIRO_CLI_VERSION}/kirocli-<arch>-linux.zip`（`KIRO_CLI_VERSION` 既定 `latest`）。
  - アーキ判定: `dpkg --print-architecture` → amd64=x86_64 / arm64=aarch64。
- **認証の永続化**: `kirocli-data` ボリュームを `/home/node/.local/share/kiro-cli` にマウントし、再ビルドでも再ログイン不要にする。

## 作成・変更するファイル

### 新規（.devcontainer/）
1. `.devcontainer/devcontainer.json`
   - `name=apuri` / `dockerComposeFile=docker-compose.yml` / `service=al` / `workspaceFolder=/workspace` / `remoteUser=node`
   - features: git, github-cli
   - VS Code 拡張・settings（必要分のみ。Prisma 等 DB 関連は削除）
   - `postCreateCommand` / `postStartCommand` / `shutdownAction=stopCompose`
2. `.devcontainer/Dockerfile`
   - `node:20-bookworm` ベース
   - apt: git/curl/wget/unzip/zip/vim/zsh/jq/sudo
   - **default-jdk（OpenJDK 17）**、`JAVA_HOME=/usr/lib/jvm/default-java`
   - **Android SDK**（cmdline-tools 14742923 → sdkmanager で platform-tools / platforms;android-35 / build-tools;35.0.0、ライセンス自動承諾、`/opt/android-sdk`、node 所有）
   - **Gradle 8.13**（`/opt` 展開 + `/usr/local/bin/gradle` シンボリックリンク）
   - pnpm（corepack）、oh-my-zsh（node ユーザ）
   - **kiro-cli インストール**（ZIP → install.sh → `/usr/local/bin` へ配置、`ARG KIRO_CLI_VERSION=latest`）
   - node ユーザに NOPASSWD sudo
3. `.devcontainer/docker-compose.yml`
   - `al` サービスのみ（`build`, `volumes: ..:/workspace:cached` と `kirocli-data:/home/node/.local/share/kiro-cli`, `ports: "${AL_PORT:-40001}:3000"`, `command: sleep infinity`）
   - `volumes: kirocli-data:`
   - 不要な aldb/alredis/smtpal/s3altal・ネットワーク依存は削除
4. `.devcontainer/setup.sh`
   - HOST 実行。インスタンス番号(1-8)入力 → `OFFSET=(N-1)*10` → `AL_PORT=40001+OFFSET`
   - `.devcontainer/.env` と ルート `.env` を生成（不要サービス変数は削除）
   - `socat-forwards.sh` は単一サービスのため生成しない（または空運用）
5. `.devcontainer/.env.tpl`
   - `AL_PORT` のみ（不要変数削除）
6. `.devcontainer/post-create.sh`
   - `git config --global --add safe.directory /workspace`
   - `package.json` があれば `pnpm install`（無ければスキップ）
   - zsh に `kiro` 関数 / PATH 追記（未ログイン時のみ SSO device-flow → `kiro-cli chat --trust-all-tools`）
   - ※ kiro 本体は Dockerfile で導入済み
7. `.devcontainer/post-start.sh`
   - kiro 既定設定の適用: `kiro-cli settings chat.defaultModel claude-opus-4.8`（必要なら agent 既定設定）
   - MinIO/socat 処理は削除

### 新規（ルート）
8. `Makefile`
   - `setup`（HOST）/ `downv`（HOST）
   - `kiro` ターゲット（いただいたスニペット準拠 / `KIRO_IDP`, `KIRO_REGION`）
   - app 用: `install` / `dev` / `build` / `test` / `lint` / `typecheck` 等は package.json 整備後に有効化（雛形として用意）
9. `devlogin`
   - HOST → `al` コンテナへ `docker exec -it -u node -w /workspace al zsh`

### 変更
10. `AGENTS.md`
    - コンテナ構成表・DevContainer ポート表を 40000 台へ更新（`al`=40001、+10 刻み 1〜8）
    - 不要サービス（aldb/smtpal/s3altal/alredis）の記述を実態（al のみ）に合わせて整理
11. `.gitignore`（必要に応じて）
    - `.env` / `.devcontainer/.env` / 生成物を無視

## Makefile kiro ターゲット（確定仕様）

```makefile
KIRO_IDP    ?= https://d-95674b0b93.awsapps.com/start
KIRO_REGION ?= ap-northeast-1

.PHONY: kiro
kiro: ## [app内] kiro-cli を起動 (未ログインなら自動でSSOログイン / opus-4.8 / trust-all)
	@kiro-cli whoami >/dev/null 2>&1 || \
		kiro-cli login --license pro --identity-provider $(KIRO_IDP) --region $(KIRO_REGION) --use-device-flow
	@kiro-cli chat --trust-all-tools
```

## ポート割当（確定）

| サービス | 用途 | inst.1 | offset |
|---|---|---|---|
| al | Node.js（アプリ／dev server） | 40001 | +10/inst |

- インスタンス 1〜8: 40001 / 40011 / 40021 / 40031 / 40041 / 40051 / 40061 / 40071

## 検証方法

- `make setup` でインスタンス番号入力 → `.env` / `.devcontainer/.env` が想定ポート（40000台）で生成されること。
- `docker compose -f .devcontainer/docker-compose.yml config` で compose 定義が妥当であること。
- VS Code「Reopen in Container」でコンテナ起動 → `which kiro-cli` でパス解決、`kiro-cli --version` が成功すること。
- `./devlogin` で HOST から zsh ログインできること。
- `make kiro` で（初回は SSO ログイン後）kiro-cli chat が起動すること。

## 確認事項の回答（確定）

1. ビルド対象 → **コンテナ内ビルド（最終決定）**。`node:20-bookworm` + OpenJDK 17 + Android SDK + Gradle を導入し、`make apk`/`make aab` で .apk/.aab を生成可能にする。エミュレーター/実機検証はホストの Android Studio。
2. `al` ポート → 内部 `:3000` にマッピングを維持（将来の dev server 用）。
3. kiro 既定エージェント → 配置しない。`chat.defaultModel` 設定のみ。
