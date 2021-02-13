package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.resource.Resource

expect class NLPCore {
    fun sentences(text: String): Array<out String>
    fun tokenize(sentence: String): Array<out String>
    fun tag(tokens: Array<out String>): Array<out String>
    fun tagEnums(tags: Array<out String>): List<Tag>
    fun lemmatize(tokens: Array<out String>, tags: Array<out String>): Array<out String>
    fun chunk(tokens: Array<out String>, tags: Array<out String>): Array<out String>
    fun spanList(tokens: Array<out String>, tags: Array<out String>, chunks: Array<out String>): List<Span>

    suspend fun waitUntilInitialized(): Resource<NLPCore>
    fun clone(): NLPCore
}