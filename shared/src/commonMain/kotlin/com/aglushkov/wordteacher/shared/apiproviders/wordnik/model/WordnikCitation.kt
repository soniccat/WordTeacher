package com.aglushkov.wordteacher.apiproviders.wordnik.model

import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class WordnikCitation(
    @SerialName("cite") val cite: String? = null,
    @SerialName("source") val source: String? = null
) : Parcelable