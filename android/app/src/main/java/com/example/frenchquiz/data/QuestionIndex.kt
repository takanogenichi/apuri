package com.example.frenchquiz.data

import kotlinx.serialization.Serializable

/** assets/questions/index.json のスキーマ（読み込み対象ファイルの一覧）。 */
@Serializable
data class QuestionIndex(
    val files: List<String> = emptyList(),
)
