package com.aglushkov.wordteacher.apiproviders.wordnik.model

import com.arkivanov.decompose.statekeeper.Parcelable
import com.arkivanov.decompose.statekeeper.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class WordnikExampleUse(
    @SerialName("position") val position: Int,
    @SerialName("text") val text: String? = null
) : Parcelable