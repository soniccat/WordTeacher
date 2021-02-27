package com.aglushkov.wordteacher.shared.model.nlp

class NLPSentenceProcessor(
    val nlpCore: NLPCore
) {
    fun process(sentence: NLPSentence) {
        sentence.tokens = nlpCore.tokenize(sentence.text)
        sentence.tags = nlpCore.tag(sentence.tokens)
        sentence.lemmas = nlpCore.lemmatize(sentence.tokens, sentence.tags)
        sentence.chunks = nlpCore.chunk(sentence.tokens, sentence.tags)
    }
}