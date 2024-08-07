package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import kotlin.math.abs

data class NLPSentence(
    var articleId: Long = 0,
    var orderId: Long = 0,
    var text: String = "",
    var tokenSpans: List<TokenSpan> = emptyList(),
    var tags: List<String> = emptyList(),
    var lemmas: List<String> = emptyList(),
    var chunks: List<String> = emptyList()
) {
    fun textIndexToNlpIndex(textIndex: Int): Int {
        return tokenSpans.spanIndexWithIndex(textIndex)
    }

    fun sliceFromTextIndex(textIndex: Int, chooseLeftIfNotFound: Boolean = false): NLPSentenceSlice? {
        var nlpIndex = textIndexToNlpIndex(textIndex)
        if (chooseLeftIfNotFound && nlpIndex < 0 && abs(nlpIndex) > 1) {
            nlpIndex = abs(nlpIndex) - 2 // convert insertion point to the span on the left
        }
        return sliceFromNlpIndex(nlpIndex)
    }

    fun sliceFromNlpIndex(nlpIndex: Int): NLPSentenceSlice? {
        if (nlpIndex < 0 || nlpIndex >= tokenSpans.size) return null
        val tokenSpan = tokenSpans[nlpIndex]

        return NLPSentenceSlice(
            text.substring(tokenSpan.start, tokenSpan.end),
            tokenSpan,
            tags[nlpIndex],
            lemmas[nlpIndex],
            chunks[nlpIndex]
        )
    }

    fun tokenStrings() = tokenSpans.map {
        text.substring(it.start, it.end)
    }

    fun tagEnums(): List<Tag> = tags.map {
        tagEnum(it)
    }

    fun tagEnum(i: Int) = tagEnum(tags[i])

    fun isAdverbNotPart(i: Int) =
        tagEnum(i).isAdverb() && when (token(i)) {
            "n't", "not" -> true
            else -> false
        }

    fun lemmaOrToken(i: Int) = if (lemmas[i] != NLPConstants.UNKNOWN_LEMMA) {
        lemmas[i]
    } else {
        token(i)
    }

    fun lemma(i: Int): String? {
        val lemma = lemmas[i]
        return if (lemma == NLPConstants.UNKNOWN_LEMMA) {
            null
        } else {
            lemma
        }
    }

    fun token(i: Int) = tokenSpans[i].let {
        text.subSequence(it.start, it.end)
    }

    fun phrases(): List<PhraseSpan> =
        phrasesAsSpanList(tokenStrings(), tags, chunks)

    override fun toString(): String {
        return tokenSpans.joinToString(separator = " ")
    }
}

data class NLPSentenceSlice(
    val tokenString: String,
    val tokenSpan: TokenSpan,
    val tag: String,
    val lemma: String,
    val chunk: String
) {
    fun tagEnum() = tagEnum(tag)
    fun partOfSpeech() = tagEnum().toPartOfSpeech()
}

private fun tagEnum(it: String) = try {
    Tag.valueOf(it)
} catch (e: Exception) {
    Tag.UNKNOWN
}

fun Tag.toPartOfSpeech() = when {
    isNoun() -> WordTeacherWord.PartOfSpeech.Noun
    isAdj() -> WordTeacherWord.PartOfSpeech.Adjective
    isAdverb() -> WordTeacherWord.PartOfSpeech.Adverb
    isPrep() -> WordTeacherWord.PartOfSpeech.Preposition
    isVerb() -> WordTeacherWord.PartOfSpeech.Verb
    isDeterminer() -> WordTeacherWord.PartOfSpeech.Determiner
    isPronoun() -> WordTeacherWord.PartOfSpeech.Pronoun
    isInterjection() -> WordTeacherWord.PartOfSpeech.Interjection
    isConjunction() -> WordTeacherWord.PartOfSpeech.Conjunction
    else -> {
        WordTeacherWord.PartOfSpeech.Undefined
    }
}