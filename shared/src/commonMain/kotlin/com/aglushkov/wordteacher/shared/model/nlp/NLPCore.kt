package com.aglushkov.wordteacher.shared.model.nlp

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface NLPLemmatizer {
    fun lemmatize(tokens: List<String>, postags: List<String>): Array<String>
}

expect class NLPCore {
    suspend fun load(dispatcher: CoroutineDispatcher = Dispatchers.Default)
    fun normalizeText(text: String): String
    fun sentenceSpans(text: String): List<SentenceSpan>
    fun tokenSpans(sentence: String): List<TokenSpan>
    fun tag(tokens: List<String>): List<String>
    fun lemmatize(tokens: List<String>, tags: List<String>): List<String>
    fun chunk(tokens: List<String>, tags: List<String>): List<String>

    suspend fun waitUntilInitialized(): NLPCore
    suspend fun waitUntilLemmatizerInitialized(): NLPLemmatizer
    fun clone(): NLPCore
}

expect fun phrasesAsSpanList(
    tokenStrings: List<String>, tags: List<String>, chunks: List<String>
): List<PhraseSpan>
