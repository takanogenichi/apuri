# 仏検2級 問題演習アプリ（FrenchQuiz）

フランス語検定2級相当の問題を「とにかく解きまくれる」Android アプリです。
DB 接続なし・オフライン完結。問題データはアプリに同梱した JSON（`assets/`）から読み込みます。

## 特長

- **選択式（4択）に統一**：前置詞・慣用表現・動詞活用・語彙のすべてを4択で出題
- **解説が豊富**：各問に日本語の詳しい解説と文法ポイントを付与
- **不正解のみ出題（復習）**：間違えた問題だけを繰り返し解ける
- **進捗のローカル保存**：正解/不正解の履歴を端末内（DataStore）に保存。外部 DB なし
- カテゴリ絞り込み・ランダム出題

## 技術スタック

- Kotlin / Jetpack Compose（Material3）
- Navigation Compose
- kotlinx.serialization（JSON）
- DataStore Preferences（進捗保存）
- 最小 SDK 26 / コンパイル SDK 34

## ディレクトリ構成

```
android/
├── settings.gradle.kts / build.gradle.kts
├── gradle/libs.versions.toml          # 依存バージョンカタログ
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── assets/questions/        # 問題データ（JSON, 同梱）
        │   │   ├── index.json           # 読み込み対象ファイルの一覧
        │   │   ├── prepositions.json    # 前置詞
        │   │   ├── idioms.json          # 慣用表現
        │   │   ├── conjugation.json     # 動詞活用
        │   │   └── vocabulary.json      # 語彙
        │   ├── java/com/example/frenchquiz/
        │   │   ├── MainActivity.kt      # NavHost（home / quiz / result）
        │   │   ├── data/                # モデル・ローダ・リポジトリ・進捗保存
        │   │   ├── domain/              # 出題ロジック（QuizEngine / QuizMode）
        │   │   └── ui/                  # 画面 + ViewModel + テーマ
        │   └── res/
        └── test/                        # QuizEngine のユニットテスト（JVM）
```

## ビルド手順（Android Studio）

> 本リポジトリには Gradle Wrapper の実行バイナリ（`gradlew` / `gradle-wrapper.jar`）を含めていません。
> 初回は Android Studio がラッパーを自動生成します（または `gradle wrapper` を実行）。

1. Android Studio（Koala 以降推奨）で `android/` フォルダを開く
2. 初回同期（Gradle Sync）で依存をダウンロード
3. デバイス/エミュレータ（API 26+）を選択して Run

### コマンドラインの場合

ラッパー生成後（または Gradle 8.9+ をインストール済みの場合）:

```bash
cd android
./gradlew assembleDebug      # APK ビルド
./gradlew test               # ユニットテスト（QuizEngineTest）
./gradlew bundleRelease      # AAB（リリース、要署名設定）
```

## 問題データの追加・編集

問題は JSON を編集するだけで増やせます（再ビルドで反映）。

- 既存ファイルに項目を追記するか、新規 JSON を作って `index.json` の `files` に追加します。
- 1項目 = 解答可能な1つの4択。スキーマは以下のとおり。

```jsonc
{
  "id": "prep-0001",            // 一意なID（進捗保存のキー）
  "category": "前置詞",          // 表示カテゴリ
  "type": "shared_pool",        // single | shared_pool | conjugation | idiom_fill
  "instruction": "( ) に入る最も適切な前置詞を選びなさい。", // 任意
  "contextText": "A : ...",     // 任意（conjugation の A 文など）
  "prompt": "Nous partons ( ) vacances ...", // 本文（( ) が空所）
  "hintJa": "日本語ヒント",       // 任意（idiom_fill で使用）
  "choices": ["en", "à", "dans", "pour"],    // 選択肢（4つ）
  "answerIndex": 0,             // 正解の choices インデックス
  "explanation": "…詳しい解説…", // 日本語解説
  "grammarPoint": "慣用的前置詞", // 任意
  "tags": ["前置詞", "en"]        // 任意
}
```

不正なデータ（`answerIndex` が範囲外、`choices` が空、`prompt` が空）や ID 重複は
読み込み時に自動的に除外されます。

## 出題形式と「選択式化」方針

仏検2級の代表的な4形式に対応しています。本来は記述式の慣用表現・動詞活用も、
誤答選択肢（活用違い・性数違い・近義語など）を用意して4択に統一しています。

| type | 元の形式 | 例 |
|---|---|---|
| `single` | 単独の穴埋め | 語彙の文脈選択 |
| `shared_pool` | 共通選択肢の穴埋め | 前置詞（小問単位に展開） |
| `conjugation` | 動詞の選択＋活用変更 | A↔B 言い換え、接続法・複合過去の性数一致など |
| `idiom_fill` | 慣用表現の穴埋め | 日本語訳ヒント付き |

## 著作権について

同梱の問題はすべてオリジナル作成です。実際の検定過去問は収録していません
（出題「形式」自体に著作権はないため、形式のみを踏襲しています）。

## 現状と今後

- 初期収録：57問（前置詞16・慣用表現14・動詞活用12・語彙15）。
- JSON への追記で 100〜200問規模へ容易に拡張できます。
- ビルド/実機検証はホストの Android Studio で行ってください
  （開発コンテナには Android SDK を含みません）。
