package com.aglushkov.wordteacher.shared.model.nlp

data class NLPSentence(
    var articleId: Long = 0,
    var orderId: Long = 0,
    var text: String = "",
    var tokens: List<String> = emptyList(),
    var tags: List<String> = emptyList(),
    var lemmas: List<String> = emptyList(),
    var chunks: List<String> = emptyList()
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