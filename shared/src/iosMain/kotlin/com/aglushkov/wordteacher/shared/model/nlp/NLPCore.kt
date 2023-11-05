package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.resource.Resource

actual class NLPCore {
    actual fun normalizeText(text: String): String {
        TODO("not implemented")
    }

    actual fun sentenceSpans(text: String): List<SentenceSpan> {
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

    actual suspend fun waitUntilInitialized(): NLPCore {
        TODO("not implemented")
    }

    actual suspend fun waitUntilLemmatizerInitialized(): NLPLemmatizer {
        TODO("not implemented")
    }

    actual fun clone(): NLPCore {
        TODO("not implemented")
    }
}

actual fun phrasesAsSpanList(
    tokenStrings: List<String>, tags: List<String>, chunks: List<String>
): List<PhraseSpan> {
    TODO("not implemented")
}
