package com.aglushkov.wordteacher.apiproviders.google.model


import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class GoogleDefinition(
    @SerialName("definition") val definition: String,
    @SerialName("example") val example: String? = null,
    @SerialName("synonyms") val synonyms: List<String>? = null
) : Parcelable

fun GoogleDefinition.asWordTeacherDefinition(): WordTeacherDefinition? {
    return WordTeacherDefinition(listOf(definition),
            if (example != null) listOf(example) else emptyList(),
            synonyms.orEmpty(),
            null)
}