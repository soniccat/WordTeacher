package com.aglushkov.wordteacher.apiproviders.google.model


import com.aglushkov.wordteacher.shared.general.extensions.merge
import com.aglushkov.wordteacher.shared.general.extensions.runIfNotEmpty
import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// All possible types were found on https://github.com/meetDeveloper/googleDictionaryAPI/issues/32
// TODO: add types not added here
@Parcelize
@Serializable
data class GoogleDefinitions(
    @SerialName("noun") val nouns: List<GoogleDefinition>? = null,
    @SerialName("proper noun") val properNouns: List<GoogleDefinition>? = null,
    @SerialName("plural noun") val pluralNouns: List<GoogleDefinition>? = null,

    @SerialName("verb") val verbs: List<GoogleDefinition>? = null,
    @SerialName("transitive verb") val transitiveVerbs: List<GoogleDefinition>? = null,
    @SerialName("intransitive verb") val intransitiveVerbs: List<GoogleDefinition>? = null,
    @SerialName("modal verb") val modalVerbs: List<GoogleDefinition>? = null,
    @SerialName("auxiliary verb") val auxiliaryVerbs: List<GoogleDefinition>? = null,

    @SerialName("adjective") val adjectives: List<GoogleDefinition>? = null,
    @SerialName("adjective & determiner") val adjectivesAndDetermines: List<GoogleDefinition>? = null,
    @SerialName("adjective & pronoun") val adjectivesAndPronouns: List<GoogleDefinition>? = null,
    @SerialName("determiner, pronoun, & adjective") val determinersPronounsAndAdjectives: List<GoogleDefinition>? = null,

    @SerialName("adverb") val adverbs: List<GoogleDefinition>? = null,
    @SerialName("interrogative adverb") val interrogativeAdverbs: List<GoogleDefinition>? = null,
    @SerialName("preposition & adverb") val prepositionsAndAdverbs: List<GoogleDefinition>? = null,
    @SerialName("adverb & adjective") val adverbsAndAdjectives: List<GoogleDefinition>? = null,
    @SerialName("conjunction & adverb") val conjunctionAndAdverb: List<GoogleDefinition>? = null,
    @SerialName("preposition, conjunction, & adverb") val prepositionsConjunctionsAndAdverb: List<GoogleDefinition>? = null,

    @SerialName("pronoun") val pronouns: List<GoogleDefinition>? = null,
    @SerialName("relative pronoun & determiner") val relativePronounsAndDeterminers: List<GoogleDefinition>? = null,

    @SerialName("determiner") val determiners: List<GoogleDefinition>? = null,
    @SerialName("abbreviation") val abbreviations: List<GoogleDefinition>? = null,
    @SerialName("exclamation") val exclamations: List<GoogleDefinition>? = null

//    @SerializedName("nom_masculin") val nomMasculins: List<GoogleDefinition>?
) : Parcelable {
    fun allNouns() = nouns.merge(properNouns).merge(pluralNouns)
    fun allVerbs() = verbs.merge(transitiveVerbs).merge(intransitiveVerbs).merge(modalVerbs).merge(auxiliaryVerbs)
    fun allAdjectives() = adjectives.merge(adjectivesAndDetermines).merge(adjectivesAndPronouns)
            .merge(determinersPronounsAndAdjectives)
    fun allAdverbs() = adverbs.merge(interrogativeAdverbs).merge(prepositionsAndAdverbs)
            .merge(adverbsAndAdjectives).merge(adverbsAndAdjectives).merge(conjunctionAndAdverb)
            .merge(prepositionsConjunctionsAndAdverb)
    fun allPronouns() = pronouns.merge(relativePronounsAndDeterminers)
    fun allDetermines() = determiners
}

fun GoogleDefinitions.asMap(): Map<WordTeacherWord.PartOfSpeech, List<WordTeacherDefinition>> {

    fun setDefinitionsToMap(map: MutableMap<WordTeacherWord.PartOfSpeech, List<WordTeacherDefinition>>,
                            partOfSpeech: WordTeacherWord.PartOfSpeech,
                            googleDefinitions: List<GoogleDefinition>?) {
        googleDefinitions?.runIfNotEmpty {
            map[partOfSpeech] = googleDefinitions.map { d -> d.asWordTeacherDefinition()!! }
        }
    }

    val map = mutableMapOf<WordTeacherWord.PartOfSpeech, List<WordTeacherDefinition>>()
    setDefinitionsToMap(map, WordTeacherWord.PartOfSpeech.Noun, allNouns())
    setDefinitionsToMap(map, WordTeacherWord.PartOfSpeech.Verb, allVerbs())
    setDefinitionsToMap(map, WordTeacherWord.PartOfSpeech.Adjective, allAdjectives())
    setDefinitionsToMap(map, WordTeacherWord.PartOfSpeech.Adverb, allAdverbs())
    setDefinitionsToMap(map, WordTeacherWord.PartOfSpeech.Pronoun, allPronouns())
    setDefinitionsToMap(map, WordTeacherWord.PartOfSpeech.Determiner, allDetermines())
    setDefinitionsToMap(map, WordTeacherWord.PartOfSpeech.Abbreviation, abbreviations)
    setDefinitionsToMap(map, WordTeacherWord.PartOfSpeech.Exclamation, exclamations)

    return map
}