package com.aglushkov.wordteacher.shared.model.nlp

data class PhraseSpan(
    override val start: Int,
    override val end: Int,
    val type: ChunkType
): NLPSpan