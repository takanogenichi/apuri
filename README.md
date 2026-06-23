# apuri

フランス語検定2級相当の問題を解けるアプリ（Android）。

開発は DevContainer 上で行い、コンテナ内でコード編集・AI 支援（kiro）・Android ネイティブビルド（.apk/.aab）まで完結できます。

## Android ビルド手順（DevContainer 内）

### 1. コンテナを再ビルドする

Dockerfile に JDK 17・Android SDK・Gradle を導入しているため、初回や Dockerfile 変更後はコンテナの再ビルドが必要です。

- VS Code のコマンドパレットで **「Dev Containers: Rebuild Container」** を実行
- ホストからコンテナへ入る場合は `./devlogin`

### 2. ビルドコマンド

コンテナ内（リポジトリルート）で以下を実行します。ビルドはプロジェクトの `android/gradlew`（Gradle Wrapper）を利用します。

| コマンド | 内容 | 生成物 |
|---|---|---|
| `make apk` | デバッグ APK をビルド（`./gradlew assembleDebug`） | `android/app/build/outputs/apk/debug/app-debug.apk` |
| `make aab` | リリース AAB をビルド（`./gradlew bundleRelease`） | `android/app/build/outputs/bundle/release/app-release.aab` |
| `make sdk-info` | JDK / Android SDK の状態を確認 | （標準出力） |

```bash
# デバッグ APK
make apk

# リリース AAB
make aab

# JDK / SDK 確認
make sdk-info
```

### 補足

- エミュレーター/実機での動作確認は、コンテナ内ではなく **ホストの Android Studio** で行います（コンテナ内ではエミュレーターは動かしません）。
- Android SDK のバージョンは `.devcontainer/Dockerfile` の `ARG`（`ANDROID_API_LEVEL` / `ANDROID_BUILD_TOOLS` / `ANDROID_CMDLINE_TOOLS_VERSION` / `GRADLE_VERSION`）で変更できます。
- DevContainer のセットアップ・kiro の起動方法など詳細は [`AGENTS.md`](AGENTS.md) を参照してください。
