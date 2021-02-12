package com.aglushkov.wordteacher.shared.model.nlp

enum class ChunkType {
    NP,     // Noun Phrase
    VP,     // Verb phrase
    PP,     // Prepositional phrase
    ADJP,   // Adjective phrase
    ADVP,   // Adverb phrase
    X;

    companion object {
        fun parse(str: String): ChunkType {
            return try {
                valueOf(str)
            } catch (e: Exception) {
                X
            }
        }
    }

    fun isNounPhrase() = this == NP
    fun isVerbPhrase() = this == VP
    fun isPrepositionalPhrase() = this == PP
}