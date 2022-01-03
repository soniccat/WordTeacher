package com.aglushkov.wordteacher.shared.features.learning.vm

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize

@Parcelize
data class SessionCardResult(
    val cardId: Long,
    val oldProgress: Float,
    var newProgress: Float = 0f,
    var isRight: Boolean = false,
) : Parcelable
