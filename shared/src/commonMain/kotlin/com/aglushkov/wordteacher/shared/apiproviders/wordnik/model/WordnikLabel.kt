package com.aglushkov.wordteacher.apiproviders.wordnik.model


import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName

@Parcelize
data class WordnikLabel(
    @SerialName("text") val text: String?,
    @SerialName("type") val type: String?
) : Parcelable