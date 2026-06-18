package com.example.frenchquiz.domain

import com.example.frenchquiz.data.QuestionSource
import com.example.frenchquiz.data.model.QuestionType
import com.example.frenchquiz.data.model.QuizItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizEngineTest {

    private fun item(id: String, category: String): QuizItem = QuizItem(
        id = id,
        category = category,
        type = QuestionType.SINGLE,
        prompt = "prompt ( ) $id",
        choices = listOf("a", "b", "c", "d"),
        answerIndex = 0,
        explanation = "exp",
    )

    private fun source(items: List<QuizItem>) = object : QuestionSource {
        override fun getAll() = items
        override fun getCategories() = items.map { it.category }.distinct()
        override fun getByCategory(category: String?) =
            if (category == null) items else items.filter { it.category == category }
        override fun getByIds(ids: Set<String>) = items.filter { it.id in ids }
    }

    private val sample = listOf(
        item("a1", "前置詞"),
        item("a2", "前置詞"),
        item("b1", "語彙"),
        item("b2", "語彙"),
        item("b3", "語彙"),
    )

    @Test
    fun all_mode_returns_everything_in_order() {
        val engine = QuizEngine(source(sample))
        val result = engine.buildSession(category = null, mode = QuizMode.ALL)
        assertEquals(listOf("a1", "a2", "b1", "b2", "b3"), result.map { it.id })
    }

    @Test
    fun category_filter_works() {
        val engine = QuizEngine(source(sample))
        val result = engine.buildSession(category = "語彙", mode = QuizMode.ALL)
        assertEquals(listOf("b1", "b2", "b3"), result.map { it.id })
    }

    @Test
    fun incorrect_only_keeps_only_incorrect_ids() {
        val engine = QuizEngine(source(sample))
        val result = engine.buildSession(
            category = null,
            mode = QuizMode.INCORRECT_ONLY,
            incorrectIds = setOf("a2", "b3"),
        )
        assertEquals(setOf("a2", "b3"), result.map { it.id }.toSet())
    }

    @Test
    fun random_is_deterministic_with_seed() {
        val engine = QuizEngine(source(sample))
        val r1 = engine.buildSession(null, QuizMode.RANDOM, shuffleSeed = 42L).map { it.id }
        val r2 = engine.buildSession(null, QuizMode.RANDOM, shuffleSeed = 42L).map { it.id }
        assertEquals(r1, r2)
        assertEquals(sample.map { it.id }.toSet(), r1.toSet())
    }

    @Test
    fun limit_caps_the_number_of_items() {
        val engine = QuizEngine(source(sample))
        val result = engine.buildSession(null, QuizMode.ALL, limit = 2)
        assertEquals(2, result.size)
    }

    @Test
    fun incorrect_count_counts_within_category() {
        val engine = QuizEngine(source(sample))
        val count = engine.incorrectCount("語彙", setOf("a2", "b1", "b3"))
        assertEquals(2, count)
        assertTrue(engine.incorrectCount("前置詞", emptySet()) == 0)
    }
}
