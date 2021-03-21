package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.resource.Resource

expect class NLPCore {
    fun sentences(text: String): List<String>
    fun tokenize(sentence: String): List<String>
    fun tag(tokens: List<String>): List<String>
    fun lemmatize(tokens: List<String>, tags: List<String>): List<String>
    fun chunk(tokens: List<String>, tags: List<String>): List<String>
    fun spanList(sentence: NLPSentence): List<Span>

    suspend fun waitUntilInitialized(): Resource<NLPCore>
    fun clone(): NLPCore
}