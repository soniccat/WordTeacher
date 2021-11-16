package com.aglushkov.wordteacher.shared.model.nlp

interface NLPSpan {
    val start: Int
    val end: Int
}

fun CharSequence.subSequence(span: NLPSpan): CharSequence {
    require(span.end <= length) {
        "The span (${span.start}, ${span.end}) is outside the given text which has length $length! ($this)"
    }
    return subSequence(span.start, span.end)
}

fun CharSequence.split(spans: List<NLPSpan>): List<CharSequence> {
    val strings = mutableListOf<CharSequence>()
    spans.forEach {
        strings += subSequence(it)
    }

    return strings
}

fun <T> List<T>.split(span: NLPSpan): List<T> {
    return subList(span.start, span.end)
}
