package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.resource.Resource

expect class NLPCore {
    fun sentenceSpans(text: String): List<SentenceSpan>
    fun tokenSpans(sentence: String): List<TokenSpan>
    fun tag(tokens: List<String>): List<String>
    fun lemmatize(tokens: List<String>, tags: List<String>): List<String>
    fun chunk(tokens: List<String>, tags: List<String>): List<String>

    suspend fun waitUntilInitialized(): Resource<NLPCore>
    fun clone(): NLPCore
}

expect fun phrasesAsSpanList(
    tokenStrings: List<String>, tags: List<String>, chunks: List<String>
): List<PhraseSpan>
