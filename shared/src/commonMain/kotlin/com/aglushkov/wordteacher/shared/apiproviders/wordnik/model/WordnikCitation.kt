package com.aglushkov.wordteacher.apiproviders.wordnik.model

import com.arkivanov.decompose.statekeeper.Parcelable
import com.arkivanov.decompose.statekeeper.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class WordnikCitation(
    @SerialName("cite") val cite: String? = null,
    @SerialName("source") val source: String? = null
) : Parcelable