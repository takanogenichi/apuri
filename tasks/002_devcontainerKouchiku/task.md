# やりたいこと

DevContainer を利用できるように構築をしなおしたい。
[axolLoop リポジトリ](https://github.com/takanogenichi/axolLoop) を参考に、同じような形式・構成にしたい。
あわせて、コンテナ内で **kiro**（Kiro CLI）を使えるようにしたい。
ポート番号は **40000 台** にしたい。

# 参考リポジトリ（axolLoop）の構成

```
.devcontainer/
├── devcontainer.json      # service=al / workspaceFolder=/workspace / remoteUser=node
│                          # features: git, github-cli
│                          # postCreateCommand / postStartCommand を実行
├── Dockerfile             # node:20-bookworm ベース
│                          # git/curl/wget/unzip/vim/zsh/socat/mysql-client/jq, pnpm, oh-my-zsh, sudo(node)
├── docker-compose.yml     # al / aldb(MySQL8) / smtpal(Mailpit) / s3altal(MinIO) / alredis(Redis7)
├── setup.sh               # HOST 実行。インスタンス番号(1-8)入力→ポート割当→.env等を自動生成
├── post-create.sh         # pnpm install / prisma generate / CLI インストール / zsh 設定
├── post-start.sh          # socat ポートフォワード起動 / MinIO バケット初期化
└── .env.tpl               # make setup で生成される .env のテンプレート

Makefile                   # setup / dev / build / test / lint / prisma 系 / コンテナログイン等
devlogin                   # HOST → al コンテナ(zsh)へログインするスクリプト
docker/database/my.cnf     # MySQL 設定
```

# 要件

## 1. DevContainer の再構築（axolLoop と同形式）
- `.devcontainer/` 一式（devcontainer.json / Dockerfile / docker-compose.yml / setup.sh / post-create.sh / post-start.sh / .env.tpl）を axolLoop と同じ形式で用意する。
- ルートに `Makefile` / `devlogin` を配置する。
- `make setup` でインスタンス番号を指定し、`.env`・`.devcontainer/.env`・`socat-forwards.sh` を自動生成する仕組みを踏襲する。
- VS Code「Reopen in Container」で起動できるようにする。

## 2. コンテナ内で kiro を使えるようにする
- 参考リポジトリは Claude Code CLI（`@anthropic-ai/claude-code`）をインストールしている。
- 本プロジェクトではこれを **kiro（Kiro CLI）** に置き換え、`post-create.sh` 等でコンテナ内に kiro をインストール／利用可能にする。
- ※ kiro のインストール方法・認証方法は要確認（questions/ で詰める）。

## 3. ポート番号を 40000 台にする
- axolLoop はインスタンス1で `29001〜29006`、インスタンス番号ごとに `+10` オフセット。
- 本プロジェクトではベースを **40000 台** に変更する（例: インスタンス1で `40001〜40006`、`+10` 刻み）。
- 各サービスの割当（案）:
  | サービス | 用途 | inst.1 ポート |
  |---|---|---|
  | al | Node.js（アプリ） | 40001 |
  | aldb | MySQL 8 | 40002 |
  | smtpal | Mailpit | 40003 |
  | s3altal | MinIO Console | 40004 |
  | alredis | Redis 7 | 40005 |
  | s3altal API | MinIO API | 40006 |

# 確認したい点（要相談）

- 本プロジェクト（apuri）のアプリ実体（フランス語検定アプリ・Android想定・DB なし）と、axolLoop 由来のサービス構成（MySQL/Redis/MinIO/Mailpit）の整合をどうするか。
  - 不要なサービス（DB 等）は削るのか、参考構成をそのまま流用するのか。
=> devconainerを構築するだけで、axolLoop 由来のサービス構成（MySQL/Redis/MinIO/Mailpit）は不要です。
- kiro（Kiro CLI）の具体的なインストール手段・コンテナ内での認証方法。
=> ## コンテナへのログイン
```bash
# HOST から app コンテナへ zsh ログイン
./devlogin
```
* app コンテナ内では `make kiro`（または `kiro`）で kiro-cli を起動できます（opus-4.8 / trust-all、初回は SSO ログイン）。
makeファイルでは以下記載を想定
# ----------------------------------------------------------------------
# kiro-cli
# ----------------------------------------------------------------------
KIRO_IDP    ?= https://d-95674b0b93.awsapps.com/start
KIRO_REGION ?= ap-northeast-1
.PHONY: kiro
kiro: ## [app内] kiro-cli を起動 (未ログインなら自動でSSOログイン / opus-4.8 / trust-all)。
@kiro-cli whoami >/dev/null 2>&1 || \
kiro-cli login --license pro --identity-provider $(KIRO_IDP) --region $(KIRO_REGION) --use-device-flow
@kiro-cli chat --trust-all-tools

、app 開発コンテナの Dockerfile のビルド時 に kiro-cli バイナリを公式配布の ZIP から取得してインストールしています（.devcontainer/Dockerfile）。該当部分は以下です。

# AWS Kiro CLI をインストール。
# ベースは Ubuntu 24.04 (glibc 2.39) のため、glibc 2.34+ を要求する標準ビルドの ZIP を利用する。
# install.sh は root 実行時 /root/.local/bin にバイナリを置くが、本 devContainer の実行ユーザは
# vscode であり /root(700) を辿れない。そのため全ユーザ実行可能な /usr/local/bin へ移動して PATH を通す。
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

ポイントを整理すると:

- 取得元: https://desktop-release.q.us-east-1.amazonaws.com/<version>/kirocli-<arch>-linux.zip（AWS公式の配布URL）。KIRO_CLI_VERSION はデフォルト latest。バージョン固定したい場合は --build-arg KIRO_CLI_VERSION=... で指定可能。
- アーキ判定: dpkg --print-architecture で amd64→x86_64 / arm64→aarch64 を切替（Apple Silicon/Intel 両対応）。
- インストール: ZIP内の install.sh --no-confirm --force を実行。root実行だと /root/.local/bin に置かれますが、devcontainerの実行ユーザは vscode で /root(700) を辿れないため、バイナリ3つ（kiro-cli / kiro-cli-chat / kiro-cli-term）を /usr/local/bin
  へ移動し、全ユーザ実行可に。
- 前提パッケージ: 同Dockerfileで curl/unzip を入れています（Ubuntu 24.04 = glibc 2.39 なので glibc 2.34+ 要求の標準ビルドZIPが使えます）。

認証とデフォルト設定はビルド時ではなく起動時に分離しています:

- 認証トークンは docker-compose.yml の kirocli-data ボリューム（/home/vscode/.local/share/kiro-cli）に永続化 → 再ビルドしても再ログイン不要。
- post-start.sh で毎起動時に ~/.kiro/agents/axol.json を配置し、kiro-cli settings chat.defaultModel claude-opus-4.8 と kiro-cli agent set-default axol を実行（opus-4.8 / trust-all）。
- ログインは make kiro（または zsh の kiro 関数）で未ログイン時のみ SSO device-flow を実行。

この方式は参考リポジトリ tapweb/AxolConvertV3 の .devcontainer/Dockerfile と同一手順を踏襲したものです。バージョン固定や別リージョン配布URLへの変更が必要なら、その方針でも調整できます。

- 既存の `AGENTS.md` 内コンテナ表（29000 台）も 40000 台に更新するか。
＝＞ 既存の AGENTS.md 内コンテナ表は 40000 台に更新する。