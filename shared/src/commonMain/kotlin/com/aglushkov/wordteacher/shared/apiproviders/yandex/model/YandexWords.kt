package com.aglushkov.wordteacher.apiproviders.yandex.model

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class YandexWords(
    @SerialName("def") val words: List<YandexWord>
) : Parcelable