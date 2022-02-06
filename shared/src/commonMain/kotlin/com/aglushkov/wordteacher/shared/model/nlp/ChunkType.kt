package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc

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
} catch (e: Exception) {
    ChunkType.X
}

fun ChunkType.toStringDesc(): StringDesc {
    val res = when(this) {
        ChunkType.NP -> MR.strings.word_phrase_noun
        ChunkType.VP -> MR.strings.word_phrase_verb
        ChunkType.PP -> MR.strings.word_phrase_preposition
        ChunkType.ADJP -> MR.strings.word_phrase_adjective
        ChunkType.ADVP -> MR.strings.word_phrase_adverb
        else -> MR.strings.word_phrase_unknown
    }
    return StringDesc.Resource(res)
}
