package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.resource.Resource

actual class NLPCore {
    actual fun sentences(text: String): List<String> {
        TODO("not implemented")
    }

    actual fun tokenSpans(sentence: String): List<TokenSpan> {
        TODO("not implemented")
    }

    actual fun tag(tokens: List<String>): List<String> {
        TODO("not implemented")
    }

    actual fun lemmatize(tokens: List<String>, tags: List<String>): List<String> {
        TODO("not implemented")
    }

    actual fun chunk(tokens: List<String>, tags: List<String>): List<String> {
        TODO("not implemented")
    }

    actual fun phrases(sentence: NLPSentence): List<PhraseSpan> {
        TODO("not implemented")
    }

    actual suspend fun waitUntilInitialized(): Resource<NLPCore> {
        TODO("not implemented")
    }

    actual fun clone(): NLPCore {
        TODO("not implemented")
    }
}