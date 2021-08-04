package com.aglushkov.wordteacher.apiproviders.wordnik.model

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class WordnikExampleUse(
    @SerialName("position") val position: Int,
    @SerialName("text") val text: String? = null
) : Parcelable