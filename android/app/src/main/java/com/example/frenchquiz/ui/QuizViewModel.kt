package com.example.frenchquiz.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.frenchquiz.data.ProgressStore
import com.example.frenchquiz.data.QuestionLoader
import com.example.frenchquiz.data.QuestionRepository
import com.example.frenchquiz.data.model.QuizItem
import com.example.frenchquiz.domain.QuizEngine
import com.example.frenchquiz.domain.QuizMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ホーム画面の状態。 */
data class HomeUiState(
    val categories: List<String> = emptyList(),
    val totalCount: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
)

/** 出題セッションの状態。 */
data class QuizUiState(
    val items: List<QuizItem> = emptyList(),
    val index: Int = 0,
    val selected: Int? = null,
    val answered: Boolean = false,
    val sessionCorrect: Int = 0,
    val sessionAnswered: Int = 0,
    val finished: Boolean = false,
) {
    val current: QuizItem? get() = items.getOrNull(index)
    val total: Int get() = items.size
    val isLast: Boolean get() = index >= items.size - 1
}

class QuizViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = QuestionRepository(QuestionLoader(app))
    private val engine = QuizEngine(repository)
    private val progressStore = ProgressStore(app)

    /** 進捗（正解/不正解 ID）を結合した状態。 */
    val homeState: StateFlow<HomeUiState> =
        combine(progressStore.correctIds, progressStore.incorrectIds) { correct, incorrect ->
            HomeUiState(
                categories = repository.getCategories(),
                totalCount = repository.getAll().size,
                correctCount = correct.size,
                incorrectCount = incorrect.size,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(
                categories = repository.getCategories(),
                totalCount = repository.getAll().size,
            ),
        )

    private val _quizState = MutableStateFlow(QuizUiState())
    val quizState: StateFlow<QuizUiState> = _quizState.asStateFlow()

    // 「不正解のみ」モードの組み立てに使うため、最新の不正解集合を保持する。
    private var latestIncorrectIds: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            progressStore.incorrectIds.collect { latestIncorrectIds = it }
        }
    }

    /** セッション開始。 */
    fun startSession(category: String?, mode: QuizMode) {
        val items = engine.buildSession(
            category = category,
            mode = mode,
            incorrectIds = latestIncorrectIds,
        )
        _quizState.value = QuizUiState(items = items)
    }

    /** 選択肢を選ぶ（解答確定前のみ変更可）。 */
    fun selectChoice(choiceIndex: Int) {
        val s = _quizState.value
        if (s.answered) return
        _quizState.value = s.copy(selected = choiceIndex)
    }

    /** 解答を確定し、進捗を記録する。 */
    fun submit() {
        val s = _quizState.value
        val current = s.current ?: return
        val selected = s.selected ?: return
        if (s.answered) return

        val isCorrect = selected == current.answerIndex
        _quizState.value = s.copy(
            answered = true,
            sessionAnswered = s.sessionAnswered + 1,
            sessionCorrect = s.sessionCorrect + if (isCorrect) 1 else 0,
        )
        viewModelScope.launch {
            progressStore.record(current.id, isCorrect)
        }
    }

    /** 次の問題へ。最後なら終了状態にする。 */
    fun next() {
        val s = _quizState.value
        if (!s.answered) return
        if (s.isLast) {
            _quizState.value = s.copy(finished = true)
        } else {
            _quizState.value = s.copy(
                index = s.index + 1,
                selected = null,
                answered = false,
            )
        }
    }

    /** セッションを破棄する。 */
    fun resetSession() {
        _quizState.value = QuizUiState()
    }

    /** すべての学習進捗を消去する。 */
    fun clearProgress() {
        viewModelScope.launch { progressStore.clear() }
    }
}
