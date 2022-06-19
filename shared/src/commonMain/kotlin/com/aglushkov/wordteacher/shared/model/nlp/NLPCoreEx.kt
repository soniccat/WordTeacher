package com.aglushkov.wordteacher.shared.model.nlp

fun NLPCore.allLemmas(word: String): List<String> {
    val tags = Tag.values()
    return lemmatize(
        tags.map { word },
        tags.map { it.value }
    ).distinct().filter { it != word && it != NLPConstants.UNKNOWN_LEMMA }
}
