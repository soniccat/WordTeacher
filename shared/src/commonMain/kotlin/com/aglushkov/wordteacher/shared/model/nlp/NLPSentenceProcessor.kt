package com.aglushkov.wordteacher.shared.model.nlp

class NLPSentenceProcessor {
    fun process(sentence: NLPSentence, nlpCore: NLPCore) {
        sentence.text = nlpCore.normalizeText(sentence.text)
        sentence.tokenSpans = nlpCore.tokenSpans(sentence.text)
        sentence.tags = nlpCore.tag(sentence.tokenStrings())
        sentence.lemmas = nlpCore.lemmatize(sentence.tokenStrings(), sentence.tags)
        sentence.chunks = nlpCore.chunk(sentence.tokenStrings(), sentence.tags)
    }

    fun processString(sentence: String, nlpCore: NLPCore): NLPSentence {
        return NLPSentence(text = sentence).apply { process(this, nlpCore) }
    }
}