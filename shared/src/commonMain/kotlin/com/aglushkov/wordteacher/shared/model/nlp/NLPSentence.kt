package com.aglushkov.wordteacher.shared.model.nlp

data class NLPSentence(
    var articleId: Long = 0,
    var orderId: Long = 0,
    var text: String = "",
    var tokenSpans: List<TokenSpan> = emptyList(),
    var tags: List<String> = emptyList(),
    var lemmas: List<String> = emptyList(),
    var chunks: List<String> = emptyList()
) {
    fun tokenStrings() = tokenSpans.map {
        text.substring(it.start, it.end)
    }

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
        tokenSpans[i].let {
            text.subSequence(it.start, it.end)
        }
    }

    override fun toString(): String {
        return tokenSpans.joinToString(separator = " ")
    }
}