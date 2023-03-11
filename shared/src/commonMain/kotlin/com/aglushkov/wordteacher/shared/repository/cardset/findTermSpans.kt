package com.aglushkov.wordteacher.shared.repository.cardset

import com.aglushkov.wordteacher.shared.model.CardSpan
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.model.nlp.NLPSpan
import com.aglushkov.wordteacher.shared.model.nlp.TokenSpan

fun findTermSpans(
    sentence: String,
    term: String,
    nlpCore: NLPCore,
    nlpSentenceProcessor: NLPSentenceProcessor
): List<CardSpan> {
    val nlpSentence = nlpSentenceProcessor.processString(sentence, nlpCore)
    val words = term.split(' ')
    var tokenI = 0
    var wordI = 0

    val foundTokenSpans: MutableList<NLPSpan> = mutableListOf()
    val termTokenSpans: MutableList<NLPSpan> = mutableListOf()

    while (tokenI < nlpSentence.tokenSpans.size && wordI < words.size) {
        val word = words[wordI]
        val token = nlpSentence.token(tokenI)
        val lemma = nlpSentence.lemma(tokenI)
        var foundSpan: NLPSpan? = null

        if (token == word || lemma == word) {
            foundSpan = nlpSentence.tokenSpans[tokenI]
        } else if (token.startsWith(word)) {
            val start = nlpSentence.tokenSpans[tokenI].start
            foundSpan = TokenSpan(start, start + word.length)
        }

        if (foundSpan != null) {
            termTokenSpans.add(foundSpan)
            if (wordI == words.size - 1) {
                foundTokenSpans.addAll(termTokenSpans)
                termTokenSpans.clear()
                wordI = 0
            } else {
                ++wordI
            }
        }

        ++tokenI
    }

    return foundTokenSpans.map {
        CardSpan(it.start, it.end)
    }
}