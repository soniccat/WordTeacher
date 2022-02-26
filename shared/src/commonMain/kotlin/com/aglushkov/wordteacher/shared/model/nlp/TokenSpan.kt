package com.aglushkov.wordteacher.shared.model.nlp

data class TokenSpan(
    override val start: Int,
    override val end: Int
): NLPSpan {
    val range: IntRange = start..end
}