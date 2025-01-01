package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.model.nlp.NLPSpan
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArticleStyle (
    @SerialName("paragraphs") val paragraphs: List<Paragraph> = emptyList(),
    @SerialName("headers") val headers: List<Header> = emptyList(),
)

@Serializable
data class Paragraph (
    @SerialName("start") override val start: Int, // first sentence index
    @SerialName("end") override val end: Int,
): NLPSpan

@Serializable
data class Header (
    @SerialName("size") val size: Int,
    @SerialName("sentenceIndex") var sentenceIndex: Int,
    @SerialName("start") override var start: Int, // first index in the sentence
    @SerialName("end") override var end: Int,
): NLPSpan
