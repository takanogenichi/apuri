# 実施計画: フランス語検定2級 問題演習アプリ

## 1. 目的・ゴール

フランス語検定2級（仏検2級）相当の選択式問題を「とにかく解きまくれる」Android アプリを作る。

- 選択式（多肢選択）で大量に解ける
- **解説が豊富**（各問題に詳しい解説を付与）
- **DB 接続なし**（問題データはアプリ内にローカル同梱）
- 対象は Android アプリ
- 実装言語は問わない

## 1.1 確定事項（ユーザ回答反映）

- 技術スタック: **Kotlin + Jetpack Compose**（特に希望なし → 提案どおり）
- UI 言語: **日本語**（問題文はフランス語、解説は日本語）
- 問題数規模: **100〜200問程度**
- 進捗保存: **過去の正解/不正解を記録**し、「**不正解のみ出題**」モードを提供
- 問題データ: **私（AI）がオリジナル問題を生成**して同梱（著作権リスク回避）

## 2. 技術選定（確定）

| 項目                 | 選定                                | 理由                                                                                           |
| -------------------- | ----------------------------------- | ---------------------------------------------------------------------------------------------- |
| プラットフォーム     | ネイティブ Android                  | 対象が Android アプリのため。AGENTS.md でも Android ビルドはホストの Android Studio で行う前提 |
| 言語                 | Kotlin                              | Android の標準。学習コスト・情報量ともに有利                                                   |
| UI                   | Jetpack Compose                     | 宣言的 UI で画面実装が高速。クイズ系の状態管理と相性が良い                                     |
| データ保持           | ローカル JSON（assets 同梱）        | 「DB 接続なし」の要件を満たす。問題追加が容易                                                  |
| 永続化（進捗・成績） | DataStore（任意）                   | 学習進捗の保存。あくまでローカル、外部 DB は使わない                                           |
| ビルド               | Gradle / Android Studio（ホスト側） | DevContainer ではコード編集、ビルド・実機検証はホスト                                          |

> 注: 上記は提案。Flutter 等の希望があれば `questions/` で確認のうえ変更可能。

## 3. データモデル（問題フォーマット案）

問題は assets 配下の JSON で管理する。

```jsonc
// assets/questions/grammar_001.json などカテゴリ別に分割
{
  "id": "gram-0001",
  "category": "文法", // 文法/語彙/長文/会話/書き取り など
  "level": "2級",
  "question": "次の( )に入る最も適切なものを選びなさい。 Je ____ à Paris.",
  "choices": ["vais", "va", "allons", "allez"],
  "answerIndex": 0,
  "explanation": "主語 Je に対応する aller の活用は vais。...（豊富な解説）",
  "grammarPoint": "直説法現在・aller の活用", // 解説の参照ポイント（任意）
  "tags": ["動詞活用", "aller"],
}
```

- カテゴリ別にファイルを分割し、起動時に読み込む
- `explanation` を充実させ、正誤に関わらず表示する

## 3.2 出題形式の拡張（ユーザ提供サンプル分析）

ユーザ提供サンプルにより、単純な「1問＋4択」では表現できない形式が判明したため、データモデルを以下に拡張する。
（サンプルは実際の検定形式と思われるため、**形式のみ踏襲し中身はオリジナルに差し替え**て生成する。サンプル自体は形式確認・監修用。）

### 形式タイプ
- `single`: 1問＋選択肢（基本形）
- `shared_pool`（パターン1相当）: 複数の小問が**共通の選択肢プール**を共有する穴埋め（前置詞など）
- `conjugation`（パターン3相当）: 動詞を**共通リストから選択＋正しく活用**。A↔B の言い換え。時制は現在/半過去/複合過去/大過去/単純未来/前未来/条件法/**接続法**を網羅。複合過去の性数一致・代名動詞を重点的に
- `idiom_fill`（パターン2相当）: 日本語訳ヒント付きの慣用表現穴埋め

### 選択式への統一方針
本アプリは「選択式」要件のため、本来は記述式のパターン2・3も多肢選択に統一する。
→ **A案採用（ユーザ承認済み 2026-06-18）**: 誤答選択肢（活用違い・性数違い・近義語など）を自動生成して完全な多肢選択にする。

### 拡張スキーマ例

```jsonc
// shared_pool（パターン1: 前置詞）
{
  "id": "prep-grp-001",
  "type": "shared_pool",
  "category": "前置詞",
  "level": "2級",
  "instruction": "( ) に入る最も適切なものを選びなさい。",
  "sharedChoices": ["à", "chez", "de", "dès", "en", "par", "pour", "sous"],
  "subQuestions": [
    { "id": "prep-grp-001-1", "text": "Ces chocolats viennent de ( ) Dupont.",
      "answer": "chez", "explanation": "店名・人名の前で「〜の店/家」は chez。..." }
  ]
}
```

```jsonc
// conjugation（パターン3: 動詞選択＋活用）
{
  "id": "conj-grp-001",
  "type": "conjugation",
  "category": "動詞活用",
  "level": "2級",
  "verbPool": ["baisser","maigrir","passer","rapprocher","rendre","réussir","se mettre","se porter"],
  "subQuestions": [
    {
      "id": "conj-grp-001-1",
      "sentenceA": "Bientôt l’été…, je dois perdre un peu de poids.",
      "sentenceB": "Il faut que je ( ) un peu avant l’été.",
      "answerLemma": "maigrir",
      "answerForm": "maigrisse",
      "tense": "接続法現在",
      "choices": ["maigrisse", "maigris", "maigrirai", "maigrissais"],  // A案: 活用違いの誤答を自動生成
      "explanation": "il faut que の後は接続法。maigrir の接続法現在 je → maigrisse。..."
    }
  ]
}
```

```jsonc
// idiom_fill（パターン2: 慣用表現）
{
  "id": "idiom-001",
  "type": "idiom_fill",
  "category": "慣用表現",
  "level": "2級",
  "text": "Ça m’a fait de la ( ) d’entendre une histoire pareille.",
  "hintJa": "あんな話を聞いてつらかった。",
  "choices": ["peine", "joie", "honte", "force"],  // A案: 多肢選択化
  "answer": "peine",
  "explanation": "faire de la peine = 悲しませる/つらい思いをさせる。..."
}
```

> UI 側は `type` に応じて表示を切り替える（共通プール／グループ表示／単問）。出題・採点・進捗保存は小問（subQuestion）単位で行う。

検定の過去問は転載しない。以下の方法で 100〜200問のオリジナルデータを用意する。

1. **AI 生成のオリジナル問題（主軸）**: 文法・語彙の概念自体に著作権はない。穴埋め・選択式の形式もありふれた形式のため、完全オリジナルの例文・選択肢・解説を生成する。
2. **動詞活用問題はテンプレ生成**: 活用規則は事実情報で著作権対象外。動詞 × 時制 × 人称で機械生成し、解説も活用規則として付与。
3. **語彙はパブリックドメインの頻出語リスト**をベースにオリジナル例文を作成。
4. **長文読解（任意・後続）**: Project Gutenberg 等のパブリックドメイン仏語テキストから抜粋し、設問はオリジナル作成。

→ 本計画では **「1 + 2」を中心に 100〜200問を生成**する。ユーザが自作する数問はサンプル/監修用に追加可能。

## 4. 画面構成（MVP）

1. **ホーム画面**
   - カテゴリ選択（文法 / 語彙 / 長文 / 会話 など）
   - 出題モード選択: 「すべて」「ランダム」「**不正解のみ（復習）**」
   - 各問題の正解/不正解の学習状況を反映
2. **出題画面（メイン）**
   - 問題文 + 選択肢（タップで回答）
   - 回答後に正誤判定＋**解説を即時表示**
   - 「次へ」で連続出題（解きまくれる体験）
3. **結果/進捗画面（任意・MVP後）**
   - セッションの正答数・正答率
   - カテゴリ別の成績

## 5. アーキテクチャ

- `data/` : JSON ローダ、Question リポジトリ（assets から読み込み）
- `domain/` : Question モデル、出題ロジック（順次/ランダム/カテゴリ絞り込み）
- `ui/` : Compose 画面（Home / Quiz / Result）+ ViewModel（状態管理）

シンプルな単方向データフロー（ViewModel が UI 状態を保持）。

## 6. 実装ステップ

1. **プロジェクト雛形作成**
   - Android プロジェクト（Kotlin + Compose）の初期化、パッケージ構成
2. **データ層**
   - 問題 JSON スキーマ確定 → サンプル問題を数十問用意
   - assets ローダ・Question リポジトリ実装
3. **出題ロジック**
   - カテゴリ絞り込み / ランダム / 順次出題
4. **UI 実装**
   - ホーム画面 → 出題画面（解答・解説表示）→ 次へ遷移
5. **解説表示の作り込み**
   - 正誤に応じた表示、解説テキストの整形（改行・強調）
6. **進捗保存（MVP 必須）**
   - DataStore に問題ごとの正解/不正解履歴を保存
   - 「不正解のみ出題」モードで履歴を参照
7. **問題データの拡充**
   - 各カテゴリの問題量を増やす（「解きまくれる」体験の担保）
8. **動作確認・ビルド**
   - ホストの Android Studio で .apk/.aab ビルド、エミュレータ/実機確認

## 7. MVP スコープ

- ホーム → 出題 → 解答 → 解説表示 → 次へ、の一連が動く
- 文法・語彙を中心に **100〜200問**のオリジナル問題を同梱
- ランダム出題・カテゴリ絞り込み・**不正解のみ出題（復習）**
- 正解/不正解履歴の保存（DataStore）

## 8. 確認事項（すべて回答済み ✅）

1. 技術スタック → Kotlin + Jetpack Compose で確定（希望なし）
2. 問題データ → 著作権セーフな **AI 生成オリジナル問題**で用意（§3.1 参照）。ユーザ自作は数問でOK
3. 進捗保存 → 正解/不正解を記録し「不正解のみ出題」を実装（MVP 必須）
4. UI 言語 → 日本語（問題文は仏語・解説は日本語）で確定
5. 問題数 → 100〜200問規模で確定

## 9. 留意点

- **著作権**: 実際の検定過去問をそのまま収録すると権利上の問題があるため、オリジナル問題または許諾済みデータを使用する
- **DB 接続なし** の要件を厳守（外部 API・外部 DB を使わずローカル完結）

## 10.sample

サンプル

パターン１前置詞

(1) Ces chocolats viennent de ( ) Dupont.
(2) Elle était ( ) la douche quand la terre a tremblé.
(3) Je me disais ( ) le début qu’il était coupable.
(4) Nous serons ( ) retour au bureau à 14 heures.
① à ② chez ③ de ④ dès ⑤ en ⑥ par ⑦ pour ⑧ sous

パターン２慣用表現穴埋め

(1) Ça m’a fait de la ( ) d’entendre une histoire pareille. 　
あんな話を聞いてつらかった。
(2) Cela n’( ) pas que vous ayez tort. 　
それでもあなたはまちがっています。
(3) Sa mémoire peut le ( ). 　
彼の記憶ちがいかもしれない。
(4) Tiens-moi au ( ) de la suite des choses. 　
その後どうなったのか教えてよ。
(5) Vous avez de la fièvre ? ( )-vous bien. 　
熱があるのですか。お大事に。

パターン３動詞の選択と活用の変更（選択肢から選択してAとBが同じ意味になるように）
特に複合過去（性と単複、代動名詞）を少し多め・接続法含む。
（現在・半過去・複合過去・大過去・単純未来・前未来・条件法・接続法）

(1)
A Bientôt l’été…, je dois perdre un peu de poids.
B Il faut que je ( ) un peu avant l’été.
(2)
A Cette proposition l’a fâchée.
B Elle ( ) en colère à cause de cette proposition.
(3)
A Contrairement à ce que je prévoyais, il a échoué à son examen.
B Je croyais qu’il ( ) son examen.
(4)
A Les formalités seront simplifiées.
B Nous ( ) les formalités plus simples.
(5)
A Vous ne pourriez pas chanter moins fort ? Nous n’arrivons pas à nous entendre.
B Si vous ( ) un peu la voix, on s’entendrait mieux !

baisser
maigrir
passer
rapprocher
rendre
réussir
se mettre
se porter

解答
パターン１
(1) 2 (2) 8 (3) 4 (4) 3
パターン２
(1) peine (2) empêche (3) tromper (4) courant (5) Soignez
パターン３
(1) maigrisse (2) s’est mise (3) réussirait (4) rendrons (5) baissiez


## 11. 追加サンプル（ユーザ提供 2回目）

> 形式は §10 と同一の3パターン。新しい `type` は不要。`conjugation` のバリエーション（不定詞・受動・倒置）を網羅対象に追加する。

### パターン1（前置詞 / shared_pool）
共通選択肢: 1 à / 2 avant / 3 de / 4 en / 5 par / 6 pour / 7 sous / 8 sur
- (1) Cette bouteille est trop grande pour deux, nous ne pouvons pas la boire ( ) entier. → **en** (4)
- (2) Elle fait très bien la cuisine quand elle est ( ) bonne humeur. → **de** (3)
- (3) Ma sœur se précipite toujours ( ) le téléphone quand il sonne. → **sur** (8)
- (4) Tu es malade ? Tant pis ( ) toi ! → **pour** (6)

### パターン2（慣用表現 / idiom_fill）
- (1) Elle est très ( ) à comprendre.（彼女、のみこみが悪いよね。）→ **lente**
- (2) J’ai lu ce livre avec beaucoup d’ ( ).（この本はすごくおもしろかった。）→ **intérêt**
- (3) Je n’aime pas voyager en bateau. J’ai le mal de ( ).（船酔いするから。）→ **mer**
- (4) Le ( ), s’il vous plaît.（満タンにしてください。）→ **plein**
- (5) Tu ( ) bien ! On allait déjeuner.（いいときに来たね。）→ **tombes**（活用動詞が答えになる例）

### パターン3（動詞活用 / conjugation）
動詞プール: demander / manquer / mettre / partager / prendre / rendre / se dépêcher / se plaindre
- (1) B Tous les enfants doivent ( ) à l’abri. → **être mis**（不定詞・受動）
- (2) B Il y a toujours quelqu’un qui ( ), on n’y peut rien. → **se plaint**（直説法現在・代名動詞）
- (3) B Ma tante a insisté pour que nous lui ( ) visite. → **rendions**（接続法現在）
- (4) B Nous ( ) votre avis. → **partageons**（直説法現在）
- (5) B Comme ( ) il de confiance en sa réponse... → **manquait**（半過去・倒置構文）

### conjugation で網羅すべき活用バリエーション（確定）
- 時制/法: 現在・半過去・複合過去・大過去・単純未来・前未来・条件法・接続法
- 複合過去の**性数一致**・**代名動詞**を重点
- **不定詞/複合不定詞・受動態**（例: être mis）
- **倒置構文**での活用（例: manquait-il）
- A案により、各小問は活用違い・性数違いの誤答を自動生成して4択化する


## 12. 実装結果（2026-06-18）

`android/` 配下に Kotlin + Jetpack Compose アプリとして実装完了（ブランチ: feature/001-french-quiz-app）。

- 画面: Home（モード/カテゴリ選択・統計・記録リセット）→ Quiz（4択・解答後に正誤配色＋解説）→ Result（正答率）
- 出題ロジック: QuizEngine（ALL / RANDOM / INCORRECT_ONLY）
- 進捗保存: DataStore Preferences（正解/不正解履歴、不正解のみ出題に利用）
- 問題データ: assets 同梱 JSON、全オリジナル **計146問**（前置詞22・慣用表現63・動詞活用27・語彙24・長文読解10）。`index.json` 経由で読み込み、JSON 追記でさらに拡張可能
- テスト: QuizEngineTest（JVM, 6ケース）。出題形式は §3.2 の4タイプ（single / shared_pool / conjugation / idiom_fill）に対応

注意: 開発コンテナには Android SDK / JDK を含まないため、ビルド・実機検証はホストの Android Studio で実施（README 参照）。本環境では JSON 妥当性チェックと静的レビューのみ実施済み。
