package com.aglushkov.wordteacher.shared.model.nlp

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList

interface NLPSpan {
    val start: Int
    val end: Int

    val length: Int
        get() = end - start
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

fun <T> ImmutableList<T>.split(span: NLPSpan): ImmutableList<T> {
    return subList(span.start, span.end)
}

fun List<NLPSpan>.spanIndexWithIndex(index: Int): Int {
    return binarySearch {
        when {
            it.end <= index -> -1
            it.start > index -> 1
            else -> 0
        }
    }
}

fun <T: NLPSpan> List<T>.spanWithIndex(index: Int): T? {
    val i = spanIndexWithIndex(index)
    return this.getOrNull(i)
}