package com.aglushkov.wordteacher.shared.model.nlp

data class SentenceSpan(
    override val start: Int,
    override val end: Int
): NLPSpan