package com.aglushkov.wordteacher.shared.model.nlp

data class PhraseSpan(val start: Int, val end: Int, val type: ChunkType)