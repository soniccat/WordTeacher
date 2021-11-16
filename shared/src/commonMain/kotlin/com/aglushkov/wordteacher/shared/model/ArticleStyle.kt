package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.model.nlp.NLPSpan
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class ArticleStyle (
    @SerialName("paragraphs") val paragraphs: List<Paragraph> = emptyList()
): Parcelable

@Parcelize
@Serializable
data class Paragraph (
    @SerialName("start") override val start: Int,
    @SerialName("end") override val end: Int,
): NLPSpan, Parcelable