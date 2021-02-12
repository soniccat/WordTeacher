package com.aglushkov.wordteacher.shared.model.nlp

enum class Tag {
    NN,     //	Noun, singular or mass
    NNS,    //	Noun, plural
    NNP,    //	Proper noun, singular
    NNPS,   //	Proper noun, plural

    VB,     //	Verb, base form
    VBD,    //	Verb, past tense
    VBG,    //	Verb, gerund or present participle
    VBN,    //	Verb, past participle
    VBP,    //	Verb, non-3rd person singular present
    VBZ,    //	Verb, 3rd person singular present

    IN,     // Preposition or subordinating conjunction

    UNKNOWN
    ;

    fun isNoun() = when (this) {
        NN, NNS, NNP, NNPS -> true
        else -> false
    }

    fun isVerb() = when (this) {
        VB, VBD, VBG, VBN, VBP, VBZ -> true
        else -> false
    }

    fun isPrep() = when (this) {
        IN -> true
        else -> false
    }
}