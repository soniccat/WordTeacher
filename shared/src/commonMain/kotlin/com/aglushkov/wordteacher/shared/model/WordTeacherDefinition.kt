package com.aglushkov.wordteacher.shared.model

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class WordTeacherDefinition(
    @SerialName("definitions") val definitions: List<String>,
    @SerialName("examples") val examples: List<String>?,
    @SerialName("synonyms") val synonyms: List<String>?,
    @SerialName("antonyms") val antonyms: List<String>?,
    @SerialName("imageUrl") val imageUrl: String?
) : Parcelable
