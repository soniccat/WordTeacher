package com.aglushkov.wordteacher.apiproviders.wordnik.model

import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
class WordnikRelatedWords (
    @SerialName("relationshipType") val relationshipType: String? = null,
    @SerialName("gram") val gram: String? = null,
    @SerialName("words") val words: List<String>
) : Parcelable