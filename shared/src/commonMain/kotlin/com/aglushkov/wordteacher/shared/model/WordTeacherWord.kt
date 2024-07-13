package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.general.serialization.EnumAsIntSerializer
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.res.MR
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class WordTeacherWord(
    @SerialName("term") val word: String,
    @SerialName("transcriptions") val transcriptions: List<String>?,
    @SerialName("definitions") val definitions: Map<PartOfSpeech, List<WordTeacherDefinition>>,
    @Transient val types: List<Config.Type> = emptyList()
) {

    private class PartOfSpeechSerializer: EnumAsIntSerializer<PartOfSpeech>(
        "partOfSpeech",
        { it.value },
        { v -> PartOfSpeech.values().first { it.value == v } }
    )

    @Serializable(with = PartOfSpeechSerializer::class)
    enum class PartOfSpeech(val value: Int) {
        Undefined(0),
        Noun(1),
        Verb(2),
        Adjective(3),
        Adverb(4),
        Pronoun(5),
        Preposition(6),
        Conjunction(7),
        Interjection(8),
        Abbreviation(9),
        Exclamation(10),
        Determiner(11),
        PhrasalVerb(12);

        companion object
    }

    companion object
}

class WordTeacherWordBuilder {
    private var word: String = ""
    private var transcriptions = mutableListOf<String>()
    private var wordDefinitions = mutableMapOf<WordTeacherWord.PartOfSpeech, MutableList<WordTeacherDefinition>>()
    private var types = mutableListOf<Config.Type>()

    // current def
    private var partOfSpeech = WordTeacherWord.PartOfSpeech.Undefined
    private var definitions = mutableListOf<String>()
    private var examples = mutableListOf<String>()
    private var synonyms = mutableListOf<String>()
    private var antonyms = mutableListOf<String>()
    private var imageUrl: String? = null

    fun setWord(v: String): WordTeacherWordBuilder {
        word = v
        return this
    }

    fun setTranscription(v: String): WordTeacherWordBuilder {
        transcriptions = mutableListOf(v)
        return this
    }

    fun setTranscriptions(v: List<String>): WordTeacherWordBuilder {
        transcriptions = v.toMutableList()
        return this
    }

    fun addType(type: Config.Type) {
        types.add(type)
    }

    fun startPartOfSpeech(partOfSpeech: WordTeacherWord.PartOfSpeech) {
        if (hasWordDefinition()) {
            addWordDefinition()
        }

        this.partOfSpeech = partOfSpeech
    }

    fun addDefinition(def: String): WordTeacherWordBuilder {
        if (definitions.isNotEmpty() && (examples.isNotEmpty() || synonyms.isNotEmpty())) {
            addWordDefinition()
        }

        definitions.add(def)
        return this
    }

    fun addExample(example: String): WordTeacherWordBuilder {
        examples.add(example)
        return this
    }

    fun addSynonym(syn: String): WordTeacherWordBuilder {
        synonyms.add(syn)
        return this
    }

    private fun hasWordDefinition() =
        definitions.isNotEmpty() ||
            examples.isNotEmpty() ||
            synonyms.isNotEmpty() ||
            antonyms.isNotEmpty() ||
            imageUrl != null

    private fun addWordDefinition() {
        val list = wordDefinitions.getOrPut(partOfSpeech) { mutableListOf() }
        list.add(
            WordTeacherDefinition(
                definitions.toList(),
                examples.toList(),
                synonyms.toList(),
                antonyms.toList(),
                imageUrl
            )
        )

        // assume that the current part of speech is valid for the next definition
        clearWordDefinition()
    }

    private fun clearWordDefinition() {
        definitions.clear()
        examples.clear()
        synonyms.clear()
        antonyms.clear()
        imageUrl = null
    }

    fun clear() {
        word = ""
        transcriptions.clear()
        wordDefinitions.clear()
        types.clear()
        partOfSpeech = WordTeacherWord.PartOfSpeech.Undefined
        clearWordDefinition()
    }

    fun build(): WordTeacherWord? {
        if (hasWordDefinition()) {
            addWordDefinition()
        }

        return if (wordDefinitions.isNotEmpty()) {
            WordTeacherWord(
                word,
                transcriptions,
                wordDefinitions.toMap(),
                types.toList()
            )
        } else {
            null
        }
    }
}

fun partOfSpeechEnum(it: String?) = if (it == null) {
    WordTeacherWord.PartOfSpeech.Undefined
} else {
    try {
        WordTeacherWord.PartOfSpeech.valueOf(it)
    } catch (e: Exception) {
        WordTeacherWord.PartOfSpeech.Undefined
    }
}

fun WordTeacherWord.PartOfSpeech.Companion.fromString(string: String?): WordTeacherWord.PartOfSpeech {
    val resultString = string?.lowercase() ?: "null"
    return when {
        resultString.contains("noun") -> WordTeacherWord.PartOfSpeech.Noun
        resultString.contains("verb") -> WordTeacherWord.PartOfSpeech.Verb
        resultString.contains("adverb") -> WordTeacherWord.PartOfSpeech.Adverb
        resultString.contains("adjective") -> WordTeacherWord.PartOfSpeech.Adjective
        resultString == "pronoun" -> WordTeacherWord.PartOfSpeech.Pronoun
        resultString == "preposition" -> WordTeacherWord.PartOfSpeech.Preposition
        resultString == "conjunction" -> WordTeacherWord.PartOfSpeech.Conjunction
        resultString == "interjection" -> WordTeacherWord.PartOfSpeech.Interjection
        resultString == "abbreviation" -> WordTeacherWord.PartOfSpeech.Abbreviation
        resultString == "determiner" -> WordTeacherWord.PartOfSpeech.Determiner
        resultString == "exclamation" -> WordTeacherWord.PartOfSpeech.Exclamation
        resultString == "фраз. гл" -> WordTeacherWord.PartOfSpeech.PhrasalVerb
        else -> {
            if (string != null) {
                //Log.d("WordTeacherWord", "New Part of Speech has found: $string")
            }
            WordTeacherWord.PartOfSpeech.Undefined
        }
    }
}

fun WordTeacherWord.PartOfSpeech.toStringDesc(): StringDesc {
    val res = when(this) {
        WordTeacherWord.PartOfSpeech.Noun -> MR.strings.word_partofspeech_noun
        WordTeacherWord.PartOfSpeech.Verb -> MR.strings.word_partofspeech_verb
        WordTeacherWord.PartOfSpeech.Adjective -> MR.strings.word_partofspeech_adjective
        WordTeacherWord.PartOfSpeech.Adverb -> MR.strings.word_partofspeech_adverb
        WordTeacherWord.PartOfSpeech.Pronoun -> MR.strings.word_partofspeech_pronoun
        WordTeacherWord.PartOfSpeech.Preposition -> MR.strings.word_partofspeech_preposition
        WordTeacherWord.PartOfSpeech.Conjunction -> MR.strings.word_partofspeech_conjunction
        WordTeacherWord.PartOfSpeech.Interjection -> MR.strings.word_partofspeech_interjection
        WordTeacherWord.PartOfSpeech.Abbreviation -> MR.strings.word_partofspeech_abbreviation
        WordTeacherWord.PartOfSpeech.Exclamation -> MR.strings.word_partofspeech_exclamation
        WordTeacherWord.PartOfSpeech.Determiner -> MR.strings.word_partofspeech_determiner
        WordTeacherWord.PartOfSpeech.PhrasalVerb -> MR.strings.word_partofspeech_phrasalVerb
        else -> MR.strings.word_partofspeech_unknown
    }
    return StringDesc.Resource(res)
}