package com.aglushkov.wordteacher.apiproviders.wordnik.model

import com.arkivanov.decompose.statekeeper.Parcelable
import com.arkivanov.decompose.statekeeper.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
class WordnikRelatedWords (
    @SerialName("relationshipType") val relationshipType: String? = null,
    @SerialName("gram") val gram: String? = null,
    @SerialName("words") val words: List<String>
) : Parcelable