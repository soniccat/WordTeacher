package com.aglushkov.wordteacher.shared.features.learning.cardteacher

data class SessionCardResult(
    val cardId: Long,
    val oldProgress: Float,
    var newProgress: Float = 0f,
    var isRight: Boolean = false,
)
