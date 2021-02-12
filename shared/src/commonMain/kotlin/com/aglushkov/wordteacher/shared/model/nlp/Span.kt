package com.aglushkov.wordteacher.shared.model.nlp

data class Span(val start: Int, val end: Int, val type: ChunkType)