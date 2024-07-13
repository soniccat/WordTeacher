package com.aglushkov.wordteacher.shared.apiproviders.yandex.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YandexSynonym(
    // Universal attributes
    @SerialName("text") val text: String,
    @SerialName("num") val num: String? = null,
    @SerialName("pos") val pos: String? = null,
    @SerialName("gen") val gender: String? = null,
    @SerialName("asp") val asp: String? = null
)