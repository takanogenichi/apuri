package com.example.frenchquiz.domain

import com.example.frenchquiz.data.QuestionSource
import com.example.frenchquiz.data.model.QuizItem

/**
 * 出題ロジック。カテゴリ・モード・進捗（不正解集合）から出題リストを組み立てる。
 * 状態は持たず、入力から純粋に出題リストを返す（テスト容易性のため）。
 */
class QuizEngine(
    private val repository: QuestionSource,
) {
    /**
     * 出題リストを構築する。
     *
     * @param category 出題カテゴリ。null なら全カテゴリ。
     * @param mode 出題モード。
     * @param incorrectIds 「不正解のみ」モードで対象とする問題 ID 集合。
     * @param limit 最大出題数（null なら制限なし）。
     * @param shuffleSeed テスト用のシード（null なら実行時ランダム）。
     */
    fun buildSession(
        category: String?,
        mode: QuizMode,
        incorrectIds: Set<String> = emptySet(),
        limit: Int? = null,
        shuffleSeed: Long? = null,
    ): List<QuizItem> {
        var items = repository.getByCategory(category)

        if (mode == QuizMode.INCORRECT_ONLY) {
            items = items.filter { it.id in incorrectIds }
        }

        items = when (mode) {
            QuizMode.RANDOM, QuizMode.INCORRECT_ONLY ->
                if (shuffleSeed != null) items.shuffled(java.util.Random(shuffleSeed)) else items.shuffled()
            QuizMode.ALL -> items
        }

        return if (limit != null && limit >= 0) items.take(limit) else items
    }

    /** 「不正解のみ」モードで出題可能な件数（UI の活性制御に使用）。 */
    fun incorrectCount(category: String?, incorrectIds: Set<String>): Int =
        repository.getByCategory(category).count { it.id in incorrectIds }
}
