package com.aglushkov.wordteacher.apiproviders.wordnik.model

import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName


@Parcelize
data class WordnikCitation(
    @SerialName("cite") val cite: String?,
    @SerialName("source") val source: String?
) : Parcelable