package com.aglushkov.wordteacher.apiproviders.yandex.model

import com.arkivanov.decompose.statekeeper.Parcelable
import com.arkivanov.decompose.statekeeper.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class YandexExample(
//    @SerialName("tr") val definitions: List<YandexDefinition>?,

    // Universal attributes
    @SerialName("text") val text: String,
    @SerialName("num") val num: String? = null,
    @SerialName("pos") val pos: String? = null,
    @SerialName("gen") val gender: String? = null,
    @SerialName("asp") val asp: String? = null
) : Parcelable