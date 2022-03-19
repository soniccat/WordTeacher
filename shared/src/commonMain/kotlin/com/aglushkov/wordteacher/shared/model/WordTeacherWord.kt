package com.aglushkov.wordteacher.shared.model

import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize

@Parcelize
data class WordTeacherWord(
    val word: String,
    val transcription: String?, // TODO: a transcription should be per definition (in WordTeacherDefinition)
    val definitions: Map<PartOfSpeech, List<WordTeacherDefinition>>,
    val types: List<Config.Type>
) : Parcelable {

    enum class PartOfSpeech {
        Undefined,
        Noun,
        Verb,
        Adjective,
        Adverb,
        Pronoun,
        Preposition,
        Conjunction,
        Interjection,
        Abbreviation,
        Exclamation,
        Determiner,
        PhrasalVerb;

        companion object
    }

    companion object
}

class WordTeacherWordBuilder {
    private var word: String = ""
    private var transcription: String? = null
    private var wordDefinitions = mutableMapOf<WordTeacherWord.PartOfSpeech, MutableList<WordTeacherDefinition>>()
    private var types = mutableListOf<Config.Type>()

    // current def
    private var partOfSpeech = WordTeacherWord.PartOfSpeech.Undefined
    private var definitions = mutableListOf<String>()
    private var examples = mutableListOf<String>()
    private var synonyms = mutableListOf<String>()
    private var imageUrl: String? = null

    fun setWord(v: String): WordTeacherWordBuilder {
        word = v
        return this
    }

    fun setTranscription(v: String): WordTeacherWordBuilder {
        transcription = v
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
            imageUrl != null

    private fun addWordDefinition() {
        val list = wordDefinitions.getOrPut(partOfSpeech) { mutableListOf() }
        list.add(
            WordTeacherDefinition(
                definitions.toList(),
                examples.toList(),
                synonyms.toList(),
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
        imageUrl = null
    }

    fun clear() {
        word = ""
        transcription = null
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
                transcription,
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
    val resultString = string?.toLowerCase() ?: "null"
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