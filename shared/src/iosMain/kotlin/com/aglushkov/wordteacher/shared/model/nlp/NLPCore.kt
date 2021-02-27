package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.resource.Resource

actual class NLPCore {
    actual fun sentences(text: String): Array<out String> {
        TODO("not implemented")
    }

    actual fun tokenize(sentence: String): Array<out String> {
        TODO("not implemented")
    }

    actual fun tag(tokens: Array<out String>): Array<out String> {
        TODO("not implemented")
    }

    actual fun lemmatize(tokens: Array<out String>, tags: Array<out String>): Array<out String> {
        TODO("not implemented")
    }

    actual fun chunk(tokens: Array<out String>, tags: Array<out String>): Array<out String> {
        TODO("not implemented")
    }

    actual fun spanList(sentence: NLPSentence): List<out Span> {
        TODO("not implemented")
    }

    actual suspend fun waitUntilInitialized(): Resource<NLPCore> {
        TODO("not implemented")
    }

    actual fun clone(): NLPCore {
        TODO("not implemented")
    }
}