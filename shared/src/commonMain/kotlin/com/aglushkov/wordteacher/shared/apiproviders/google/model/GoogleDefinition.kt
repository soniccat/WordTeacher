package com.aglushkov.wordteacher.apiproviders.google.model


import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
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