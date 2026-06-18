package com.example.frenchquiz.data

import android.content.Context
import com.example.frenchquiz.data.model.QuizItem
import kotlinx.serialization.json.Json

/**
 * assets 配下の問題 JSON を読み込むローダ。
 *
 * `assets/questions/index.json`（マニフェスト）に列挙された各 JSON ファイルを読み、
 * [QuizItem] のリストへデシリアライズする。DB やネットワークは使用しない。
 */
class QuestionLoader(
    private val context: Context,
    private val json: Json = defaultJson,
) {
    /**
     * すべての問題を読み込む。重複 ID と不正データ（[QuizItem.isValid] が false）は除外する。
     */
    fun loadAll(): List<QuizItem> {
        val manifest = runCatching {
            val text = readAsset(INDEX_PATH)
            json.decodeFromString<QuestionIndex>(text)
        }.getOrElse { QuestionIndex(emptyList()) }

        val seenIds = mutableSetOf<String>()
        val result = mutableListOf<QuizItem>()
        for (path in manifest.files) {
            val items = runCatching {
                json.decodeFromString<List<QuizItem>>(readAsset(path))
            }.getOrElse { emptyList() }
            for (item in items) {
                if (item.isValid() && seenIds.add(item.id)) {
                    result += item
                }
            }
        }
        return result
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    companion object {
        private const val INDEX_PATH = "questions/index.json"

        val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
