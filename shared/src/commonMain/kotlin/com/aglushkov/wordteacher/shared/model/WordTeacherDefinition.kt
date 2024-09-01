package com.aglushkov.wordteacher.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WordTeacherDefinition(
    @SerialName("definitions") val definitions: List<String>,
    @SerialName("examples") val examples: List<String>?,
    @SerialName("synonyms") val synonyms: List<String>?,
    @SerialName("antonyms") val antonyms: List<String>?,
    @SerialName("imageUrl") val imageUrl: String?,
    @SerialName("labels") val labels: List<String>?, // from wiktionary
)
