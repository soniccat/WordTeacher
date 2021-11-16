package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.resource.Resource

actual class NLPCore {
    actual suspend fun waitUntilInitialized(): Resource<NLPCore> = Resource.Uninitialized()

    actual fun sentenceSpans(text: String) = emptyList<SentenceSpan>()
    actual fun tokenSpans(sentence: String) = emptyList<TokenSpan>()
    actual fun tag(tokens: List<String>) = emptyList<String>()
    actual fun lemmatize(tokens: List<String>, tags: List<String>) = emptyList<String>()
    actual fun chunk(tokens: List<String>, tags: List<String>) = emptyList<String>()

    actual fun phrases(sentence: NLPSentence): List<PhraseSpan> = emptyList()

    // to be able to work with NLPCore in a separate thread
    actual fun clone(): NLPCore {
        TODO("isn't implemented")
    }
}
