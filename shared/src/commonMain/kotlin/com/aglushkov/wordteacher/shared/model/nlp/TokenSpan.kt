package com.aglushkov.wordteacher.shared.model.nlp

data class TokenSpan(val start: Int, val end: Int) {
    val range: IntRange = start..end
}