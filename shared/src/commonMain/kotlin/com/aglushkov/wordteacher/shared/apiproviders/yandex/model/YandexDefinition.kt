package com.aglushkov.wordteacher.apiproviders.yandex.model

import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class YandexDefinition(
    @SerialName("ex") val examples: List<YandexExample>? = null,
    @SerialName("mean") val meanings: List<YandexMeaning>? = null,
    @SerialName("syn") val synonyms: List<YandexSynonym>? = null,

    // Universal attributes
    @SerialName("text") val text: String,
    @SerialName("num") val num: String? = null,
    @SerialName("pos") val pos: String? = null,
    @SerialName("gen") val gender: String? = null,
    @SerialName("asp") val asp: String? = null
) : Parcelable

fun YandexDefinition.asWordTeacherDefinition(): WordTeacherDefinition? {
    val resultExamples = examples.orEmpty().map { it.text }
    val resultSynonyms = synonyms.orEmpty().map { it.text }
    // TODO: support meanings for non english definitions

    return WordTeacherDefinition(listOf(text),
            resultExamples,
            resultSynonyms,
            null)
}