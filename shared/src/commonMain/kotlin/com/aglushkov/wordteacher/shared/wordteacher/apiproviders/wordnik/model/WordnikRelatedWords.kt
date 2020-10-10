package com.aglushkov.wordteacher.apiproviders.wordnik.model

import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName

@Parcelize
class WordnikRelatedWords (
    @SerialName("relationshipType") val relationshipType: String?,
    @SerialName("gram") val gram: String?,
    @SerialName("words") val words: List<String>
) : Parcelable