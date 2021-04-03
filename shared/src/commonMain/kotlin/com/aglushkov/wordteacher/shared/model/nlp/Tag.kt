package com.aglushkov.wordteacher.shared.model.nlp

enum class Tag(val value: String) {
    NN("NN"),       // Noun, singular or mass
    NNS("NNS"),     // Noun, plural
    NNP("NNP"),     // Proper noun, singular
    NNPS("NNPS"),   // Proper noun, plural

    VB("VB"),       // Verb, base form
    VBD("VBD"),     // Verb, past tense
    VBG("VBG"),     // Verb, gerund or present participle
    VBN("VBN"),     // Verb, past participle
    VBP("VBP"),     // Verb, non-3rd person singular present
    VBZ("VBZ"),     // Verb, 3rd person singular present

    JJ("JJ"),       // Adjective
    JJR("JJR"),     // Adjective, comparative
    JJS("JJS"),     // Adjective, superlative

    RB("RB"),       // Adverb
    RBR("RBR"),     // Adverb, comparative
    RBS("RBS"),     // Adverb, superlative
    WRB("WRB"),     // Wh-adverb

    CC("CC"),       // Coordinating conjunction

    IN("IN"),       // Preposition or subordinating conjunction

    DT("DT"),       // Determiner
    PDT("PDT"),     // Predeterminer
    WDT("WDT"),     // Wh-determiner

    PRP("PRP"),     // Personal pronoun
    PSP("PRP$"),    // Possessive pronoun
    WP("WP"),       // Wh-pronoun
    PWP("WP$"),     // Possessive wh-pronoun

    UH("UH"),       // Interjection

    UNKNOWN("UNKNOWN")
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

    fun isConjunction() = this == CC

    fun isAdj() = when (this) {
        JJ, JJR, JJS -> true
        else -> false
    }

    fun isAdverb() = when (this) {
        RB, RBR, RBS, WRB -> true
        else -> false
    }

    fun isDeterminer() = when (this) {
        DT, PDT, WDT -> true
        else -> false
    }

    fun isPronoun() = when (this) {
        PRP, PSP, WP, PWP -> true
        else -> false
    }

    fun isInterjection() = this == Tag.UH
}