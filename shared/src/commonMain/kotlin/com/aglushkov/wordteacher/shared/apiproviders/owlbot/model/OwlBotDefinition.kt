package com.aglushkov.wordteacher.shared.apiproviders.owlbot.model

import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class OwlBotDefinition(
    @SerialName("definition") val definition: String?,
//    @SerializedName("emoji") val emoji: String?,
    @SerialName("example") val example: String?,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("type") val type: String?
) : Parcelable

fun OwlBotDefinition.asWordTeacherDefinition(): WordTeacherDefinition? {
    if (definition == null) return null

    val resultExamples = example?.let {
        listOf(it)
    } ?: run {
        emptyList<String>()
    }

    return WordTeacherDefinition(
        listOf(definition),
        resultExamples,
        emptyList(),
        emptyList(),
        imageUrl
    )
}