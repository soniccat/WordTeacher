package com.aglushkov.wordteacher.shared.model.nlp

class NLPSentence {
    companion object {
    }

    private val core: NLPCore

    var tokens: Array<out String> = emptyArray()
    var tags: Array<out String> = emptyArray()
    var lemmas: Array<out String> = emptyArray()
    var chunks: Array<out String> = emptyArray()

    constructor(
        text: String,
        core: NLPCore
    ) {
        this.core = core
        load(text)
    }

    constructor(
        tokens: Array<out String>,
        tags: Array<out String>,
        lemmas: Array<out String>,
        chunks: Array<out String>,
        core: NLPCore
    ) {
        this.tokens = tokens
        this.tags = tags
        this.lemmas = lemmas
        this.chunks = chunks
        this.core = core
    }

    private fun load(text: String) {
        tokens = core.tokenize(text)
        tags = core.tag(tokens)
        lemmas = core.lemmatize(tokens, tags)
        chunks = core.chunk(tokens, tags)
    }

    fun tagEnums() = core.tagEnums(tags)
    fun spanList() = core.spanList(tokens, tags, chunks)

    fun lemmaOrToken(i: Int) = if (lemmas[i] != NLPConstants.UNKNOWN_LEMMA) {
        lemmas[i]
    } else {
        tokens[i]
    }

    override fun toString(): String {
        return tokens.joinToString(separator = " ")
    }
}