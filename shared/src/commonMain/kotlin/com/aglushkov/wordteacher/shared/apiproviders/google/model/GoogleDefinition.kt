package com.aglushkov.wordteacher.apiproviders.google.model


import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName

@Parcelize
data class GoogleDefinition(
    @SerialName("definition") val definition: String,
    @SerialName("example") val example: String?,
    @SerialName("synonyms") val synonyms: List<String>?
) : Parcelable

fun GoogleDefinition.asWordTeacherDefinition(): WordTeacherDefinition? {
    return WordTeacherDefinition(listOf(definition),
            if (example != null) listOf(example) else emptyList(),
            synonyms.orEmpty(),
            null)
}