package com.aglushkov.wordteacher.shared.model.nlp

class NLPSentence(
    private val core: NLPCore,
    var articleId: Long = 0,
    var orderId: Long = 0,
    var text: String = "",
    var tokens: Array<out String> = emptyArray(),
    var tags: Array<out String> = emptyArray(),
    var lemmas: Array<out String> = emptyArray(),
    var chunks: Array<out String> = emptyArray()
) {
    init {
        load()
    }

    private fun load() {
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