package com.example.frenchquiz.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 各問題の最新の正誤状態。 */
enum class AnswerStatus { UNSEEN, CORRECT, INCORRECT }

/** DataStore 拡張プロパティ（プロセス内で単一インスタンス）。 */
private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(name = "quiz_progress")

/**
 * 問題ごとの正解/不正解履歴をローカル保存する（DataStore Preferences）。
 *
 * - correctIds: 直近で正解した問題 ID の集合
 * - incorrectIds: 直近で不正解だった問題 ID の集合
 *
 * 同一問題は「最新の結果」のみを保持する（正解したら不正解集合から除く、その逆も同様）。
 * これにより「不正解のみ出題（復習）」が実現できる。外部 DB は使用しない。
 */
class ProgressStore(private val context: Context) {

    private val correctKey = stringSetPreferencesKey("correct_ids")
    private val incorrectKey = stringSetPreferencesKey("incorrect_ids")

    val correctIds: Flow<Set<String>> =
        context.progressDataStore.data.map { it[correctKey] ?: emptySet() }

    val incorrectIds: Flow<Set<String>> =
        context.progressDataStore.data.map { it[incorrectKey] ?: emptySet() }

    /** 1問の解答結果を記録する。最新結果が優先される。 */
    suspend fun record(questionId: String, isCorrect: Boolean) {
        context.progressDataStore.edit { prefs ->
            val correct = (prefs[correctKey] ?: emptySet()).toMutableSet()
            val incorrect = (prefs[incorrectKey] ?: emptySet()).toMutableSet()
            if (isCorrect) {
                correct += questionId
                incorrect -= questionId
            } else {
                incorrect += questionId
                correct -= questionId
            }
            prefs[correctKey] = correct
            prefs[incorrectKey] = incorrect
        }
    }

    /** すべての進捗を消去する。 */
    suspend fun clear() {
        context.progressDataStore.edit { it.clear() }
    }
}
