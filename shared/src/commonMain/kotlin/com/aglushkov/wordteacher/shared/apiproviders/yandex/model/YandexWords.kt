package com.aglushkov.wordteacher.apiproviders.yandex.model

import com.arkivanov.decompose.statekeeper.Parcelable
import com.arkivanov.decompose.statekeeper.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class YandexWords(
    @SerialName("def") val words: List<YandexWord>
) : Parcelable