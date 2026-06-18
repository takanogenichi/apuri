package com.example.frenchquiz.data

import com.example.frenchquiz.data.model.QuizItem

/**
 * 問題データ取得の抽象。QuizEngine をユニットテスト可能にするために導入。
 * 本番では [QuestionRepository] が実装する。
 */
interface QuestionSource {
    fun getAll(): List<QuizItem>
    fun getCategories(): List<String>
    fun getByCategory(category: String?): List<QuizItem>
    fun getByIds(ids: Set<String>): List<QuizItem>
}
