package com.aglushkov.wordteacher.shared.model.nlp

class NLPSentence(
    var articleId: Long = 0,
    var orderId: Long = 0,
    var text: String = "",
    var tokens: Array<out String> = emptyArray(),
    var tags: Array<out String> = emptyArray(),
    var lemmas: Array<out String> = emptyArray(),
    var chunks: Array<out String> = emptyArray()
) {
    fun tagEnums(): List<Tag> = tags.map {
        try {
            Tag.valueOf(it)
        } catch (e: Exception) {
            Tag.UNKNOWN
        }
    }

    fun lemmaOrToken(i: Int) = if (lemmas[i] != NLPConstants.UNKNOWN_LEMMA) {
        lemmas[i]
    } else {
        tokens[i]
    }

    override fun toString(): String {
        return tokens.joinToString(separator = " ")
    }
}