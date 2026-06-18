package com.example.frenchquiz.data

import com.example.frenchquiz.data.model.QuizItem

/**
 * 問題データへのアクセスを担うリポジトリ。
 * assets からの読み込み結果をメモリにキャッシュする（問題数は数百件規模で十分メモリに乗る）。
 */
class QuestionRepository(
    private val loader: QuestionLoader,
) : QuestionSource {
    @Volatile
    private var cache: List<QuizItem>? = null

    /** 全問題を取得（初回のみ assets を読み込む）。 */
    override fun getAll(): List<QuizItem> {
        cache?.let { return it }
        return synchronized(this) {
            cache ?: loader.loadAll().also { cache = it }
        }
    }

    /** 出題カテゴリの一覧（出現順を保持）。 */
    override fun getCategories(): List<String> =
        getAll().map { it.category }.distinct()

    /** 指定カテゴリの問題（null の場合は全件）。 */
    override fun getByCategory(category: String?): List<QuizItem> {
        val all = getAll()
        return if (category == null) all else all.filter { it.category == category }
    }

    /** ID 集合に一致する問題を取得。 */
    override fun getByIds(ids: Set<String>): List<QuizItem> =
        getAll().filter { it.id in ids }
}
