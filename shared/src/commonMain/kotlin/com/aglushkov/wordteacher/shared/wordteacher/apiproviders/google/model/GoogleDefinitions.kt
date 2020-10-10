package com.aglushkov.wordteacher.apiproviders.google.model


import com.aglushkov.wordteacher.shared.general.extensions.merge
import com.aglushkov.wordteacher.shared.general.extensions.runIfNotEmpty
import com.aglushkov.wordteacher.shared.wordteacher.model.WordTeacherDefinition
import com.aglushkov.wordteacher.shared.wordteacher.model.WordTeacherWord
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName

// All possible types were found on https://github.com/meetDeveloper/googleDictionaryAPI/issues/32
// TODO: add types not added here
@Parcelize
data class GoogleDefinitions(
    @SerialName("noun") val nouns: List<GoogleDefinition>?,
    @SerialName("proper noun") val properNouns: List<GoogleDefinition>?,
    @SerialName("plural noun") val pluralNouns: List<GoogleDefinition>?,

    @SerialName("verb") val verbs: List<GoogleDefinition>?,
    @SerialName("transitive verb") val transitiveVerbs: List<GoogleDefinition>?,
    @SerialName("intransitive verb") val intransitiveVerbs: List<GoogleDefinition>?,
    @SerialName("modal verb") val modalVerbs: List<GoogleDefinition>?,
    @SerialName("auxiliary verb") val auxiliaryVerbs: List<GoogleDefinition>?,

    @SerialName("adjective") val adjectives: List<GoogleDefinition>?,
    @SerialName("adjective & determiner") val adjectivesAndDetermines: List<GoogleDefinition>?,
    @SerialName("adjective & pronoun") val adjectivesAndPronouns: List<GoogleDefinition>?,
    @SerialName("determiner, pronoun, & adjective") val determinersPronounsAndAdjectives: List<GoogleDefinition>?,

    @SerialName("adverb") val adverbs: List<GoogleDefinition>?,
    @SerialName("interrogative adverb") val interrogativeAdverbs: List<GoogleDefinition>?,
    @SerialName("preposition & adverb") val prepositionsAndAdverbs: List<GoogleDefinition>?,
    @SerialName("adverb & adjective") val adverbsAndAdjectives: List<GoogleDefinition>?,
    @SerialName("conjunction & adverb") val conjunctionAndAdverb: List<GoogleDefinition>?,
    @SerialName("preposition, conjunction, & adverb") val prepositionsConjunctionsAndAdverb: List<GoogleDefinition>?,

    @SerialName("pronoun") val pronouns: List<GoogleDefinition>?,
    @SerialName("relative pronoun & determiner") val relativePronounsAndDeterminers: List<GoogleDefinition>?,

    @SerialName("determiner") val determiners: List<GoogleDefinition>?,
    @SerialName("abbreviation") val abbreviations: List<GoogleDefinition>?,
    @SerialName("exclamation") val exclamations: List<GoogleDefinition>?

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