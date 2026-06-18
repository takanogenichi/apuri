package com.example.frenchquiz.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 出題形式。仏検2級の代表的な4形式を表す。
 * 本アプリはすべて「4択」に統一する（記述式の慣用表現・動詞活用も誤答選択肢を用意して選択式化）。
 */
@Serializable
enum class QuestionType {
    /** 単独の穴埋め4択 */
    @SerialName("single")
    SINGLE,

    /** 前置詞などの共通選択肢グループ問題（本アプリでは小問単位の4択へ展開済み） */
    @SerialName("shared_pool")
    SHARED_POOL,

    /** 動詞の選択＋活用変更（A↔B 言い換え）。活用違いの誤答を選択肢に含む */
    @SerialName("conjugation")
    CONJUGATION,

    /** 日本語訳ヒント付きの慣用表現穴埋め */
    @SerialName("idiom_fill")
    IDIOM_FILL,

    /** 長文読解（本文 + 設問の4択） */
    @SerialName("reading")
    READING,
}

/**
 * 問題1問（1つの解答可能な4択）を表す共通モデル。
 *
 * グループ形式（前置詞の共通選択肢など）も、アプリ内では小問ごとに本モデルへ
 * フラット化して保持する。これにより出題・採点・進捗保存をすべて小問単位で統一できる。
 *
 * @property id 一意なID（進捗保存のキー）
 * @property category 表示用カテゴリ（例: 前置詞 / 慣用表現 / 動詞活用 / 語彙）
 * @property type 出題形式
 * @property instruction 設問の指示文（任意。例:「( ) に入る最も適切なものを選びなさい。」）
 * @property contextText 補助文（任意。conjugation の A 文など、解答に直接含めない参考文）
 * @property prompt 解答対象の本文（( ) を含むフランス語文）
 * @property hintJa 日本語ヒント／訳（任意。idiom_fill で使用）
 * @property choices 選択肢（4択を想定）
 * @property answerIndex 正解の choices インデックス
 * @property explanation 解説（日本語、豊富に）
 * @property grammarPoint 文法ポイントのラベル（任意）
 * @property tags 検索・分類用タグ（任意）
 */
@Serializable
data class QuizItem(
    val id: String,
    val category: String,
    val type: QuestionType,
    val instruction: String? = null,
    val contextText: String? = null,
    val prompt: String,
    val hintJa: String? = null,
    val choices: List<String>,
    val answerIndex: Int,
    val explanation: String,
    val grammarPoint: String? = null,
    val tags: List<String> = emptyList(),
) {
    /** 正解の文字列 */
    val answerText: String get() = choices.getOrElse(answerIndex) { "" }

    /** データ整合性チェック（選択肢数・正解インデックス範囲） */
    fun isValid(): Boolean =
        choices.isNotEmpty() && answerIndex in choices.indices && prompt.isNotBlank()
}
