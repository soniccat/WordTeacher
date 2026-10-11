package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import wordteacher.shared.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import wordteacher.shared.generated.resources.word_phrase_adjective
import wordteacher.shared.generated.resources.word_phrase_adverb
import wordteacher.shared.generated.resources.word_phrase_noun
import wordteacher.shared.generated.resources.word_phrase_preposition
import wordteacher.shared.generated.resources.word_phrase_unknown
import wordteacher.shared.generated.resources.word_phrase_verb

enum class ChunkType {
    NP,     // Noun Phrase
    VP,     // Verb phrase
    PP,     // Prepositional phrase
    ADJP,   // Adjective phrase
    ADVP,   // Adverb phrase
    X;

    fun isNounPhrase() = this == NP
    fun isVerbPhrase() = this == VP
    fun isPrepositionalPhrase() = this == PP
}

fun chunkEnum(it: String) = try {
    ChunkType.valueOf(it)
} catch (e: Throwable) {
    ChunkType.X
}

fun ChunkType.toStringResource(): StringResourceWrapper {
    val res = when(this) {
        ChunkType.NP -> Res.string.word_phrase_noun
        ChunkType.VP -> Res.string.word_phrase_verb
        ChunkType.PP -> Res.string.word_phrase_preposition
        ChunkType.ADJP -> Res.string.word_phrase_adjective
        ChunkType.ADVP -> Res.string.word_phrase_adverb
        else -> Res.string.word_phrase_unknown
    }
    return StringResourceWrapper.String(res)
}
