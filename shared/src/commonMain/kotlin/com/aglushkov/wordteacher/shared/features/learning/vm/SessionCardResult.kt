package com.aglushkov.wordteacher.shared.features.learning.vm

import kotlinx.serialization.Serializable

@Serializable
data class SessionCardResult(
    val cardId: Long,
    val oldProgress: Float,
    val newProgress: Float = 0f,
    val isRight: Boolean = false,
)
