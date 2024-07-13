package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.model.nlp.NLPSpan
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArticleStyle (
    @SerialName("paragraphs") val paragraphs: List<Paragraph> = emptyList()
)

@Serializable
data class Paragraph (
    @SerialName("start") override val start: Int,
    @SerialName("end") override val end: Int,
): NLPSpan