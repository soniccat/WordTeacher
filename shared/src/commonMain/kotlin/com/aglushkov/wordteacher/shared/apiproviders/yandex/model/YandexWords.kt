package com.aglushkov.wordteacher.shared.apiproviders.yandex.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class YandexWords(
    @SerialName("def") val words: List<YandexWord>
)