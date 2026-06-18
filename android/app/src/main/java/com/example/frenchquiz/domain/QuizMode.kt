package com.example.frenchquiz.domain

/** 出題モード。 */
enum class QuizMode {
    /** 全問（出題順そのまま） */
    ALL,

    /** ランダム出題 */
    RANDOM,

    /** 不正解のみ（復習） */
    INCORRECT_ONLY,
}
