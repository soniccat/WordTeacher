package com.aglushkov.wordteacher.apiproviders.wordnik.model


import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class WordnikExampleUse(
    @SerialName("position") val position: Int,
    @SerialName("text") val text: String? = null
) : Parcelable