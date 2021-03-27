package com.aglushkov.wordteacher.shared.model.nlp

class NLPSentenceProcessor(
    val nlpCore: NLPCore
) {
    fun process(sentence: NLPSentence) {
        sentence.tokenSpans = nlpCore.tokenSpans(sentence.text)
        sentence.tags = nlpCore.tag(sentence.tokenStrings())
        sentence.lemmas = nlpCore.lemmatize(sentence.tokenStrings(), sentence.tags)
        sentence.chunks = nlpCore.chunk(sentence.tokenStrings(), sentence.tags)
    }
}