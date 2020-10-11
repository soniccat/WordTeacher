package com.aglushkov.wordteacher.apiproviders.yandex.model


import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName

@Parcelize
data class YandexExample(
//    @SerialName("tr") val definitions: List<YandexDefinition>?,

    // Universal attributes
    @SerialName("text") val text: String,
    @SerialName("num") val num: String?,
    @SerialName("pos") val pos: String?,
    @SerialName("gen") val gender: String?,
    @SerialName("asp") val asp: String?
) : Parcelable