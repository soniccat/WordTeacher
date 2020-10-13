package com.aglushkov.wordteacher.apiproviders.yandex.model

import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class YandexWords(
    @SerialName("def") val words: List<YandexWord>
) : Parcelable