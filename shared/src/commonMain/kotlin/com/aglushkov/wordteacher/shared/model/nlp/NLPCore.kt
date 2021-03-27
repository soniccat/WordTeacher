package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.resource.Resource

expect class NLPCore {
    fun sentences(text: String): List<String>
    fun tokenSpans(sentence: String): List<TokenSpan>
    fun tag(tokens: List<String>): List<String>
    fun lemmatize(tokens: List<String>, tags: List<String>): List<String>
    fun chunk(tokens: List<String>, tags: List<String>): List<String>
    fun phrases(sentence: NLPSentence): List<PhraseSpan>

    suspend fun waitUntilInitialized(): Resource<NLPCore>
    fun clone(): NLPCore
}