package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.general.extensions.trimNonLetterNonDigit
import com.aglushkov.wordteacher.shared.general.extensions.unbreakable
import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import com.aglushkov.wordteacher.shared.general.serialization.EnumAsIntSerializer
import com.aglushkov.wordteacher.shared.model.label_map.labelMap
import com.aglushkov.wordteacher.shared.model.label_map.labelToPartOfSpeechMap
import wordteacher.shared.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import wordteacher.shared.generated.resources.word_partofspeech_abbreviation
import wordteacher.shared.generated.resources.word_partofspeech_adjective
import wordteacher.shared.generated.resources.word_partofspeech_adverb
import wordteacher.shared.generated.resources.word_partofspeech_conjunction
import wordteacher.shared.generated.resources.word_partofspeech_determiner
import wordteacher.shared.generated.resources.word_partofspeech_exclamation
import wordteacher.shared.generated.resources.word_partofspeech_interjection
import wordteacher.shared.generated.resources.word_partofspeech_noun
import wordteacher.shared.generated.resources.word_partofspeech_phrasalVerb
import wordteacher.shared.generated.resources.word_partofspeech_preposition
import wordteacher.shared.generated.resources.word_partofspeech_pronoun
import wordteacher.shared.generated.resources.word_partofspeech_unknown
import wordteacher.shared.generated.resources.word_partofspeech_verb
import kotlin.collections.mutableListOf

@Serializable
data class WordTeacherWord(
    @SerialName("term") val word: String,
    @SerialName("transcriptions") val transcriptions: List<String>?,
    @SerialName("definitions") val definitions: LinkedHashMap<PartOfSpeech, List<WordTeacherDefinition>>,
    @Transient val sourceNames: List<String> = emptyList(),
    @SerialName("audioFiles") val audioFiles: List<AudioFile> = emptyList(),
) {
    @Serializable
    data class AudioFile(
        @SerialName("url") val url: String,
        @SerialName("accent") val accent: String?,
        @SerialName("transcription") val transcription: String?,
        @SerialName("text") val text: String?,
    )

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
    private var sourceNames = mutableListOf<String>()
    private var transcriptions = mutableListOf<String>()
    private var wordDefinitions = mutableMapOf<WordTeacherWord.PartOfSpeech, MutableList<WordTeacherDefinition>>()

    // current def
    private var partOfSpeech = WordTeacherWord.PartOfSpeech.Undefined
    private var definitions = mutableListOf<String>()
    private var examples = mutableListOf<String>()
    private var synonyms = mutableListOf<String>()
    private var antonyms = mutableListOf<String>()
    private var imageUrl: String? = null
    private var labels = mutableListOf<String>()
    private var isSynonymsBlock = false
    private var isAntonymsBlock = false

    fun setWord(v: String): WordTeacherWordBuilder {
        word = v
        return this
    }

    fun addSourceName(n: String): WordTeacherWordBuilder {
        sourceNames.add(n.unbreakable())
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

    fun addLabel(label: String) {
        val correctedLabel = labelMap[label] ?: label
        if (labels.indexOf(correctedLabel) == -1) {
            labels.add(correctedLabel)
        }
    }

    fun startPartOfSpeech(partOfSpeech: WordTeacherWord.PartOfSpeech) {
        if (hasWordDefinition()) {
            addWordDefinition()
        }

        this.partOfSpeech = partOfSpeech
    }

    fun hasPartOfSpeech() =
        partOfSpeech != WordTeacherWord.PartOfSpeech.Undefined

    fun addDefinition(def: String): WordTeacherWordBuilder {
        if (definitions.isNotEmpty()) {
            addWordDefinition()
        }

//        val oldFashionedLabel = "[OLD-FASHIONED]"
//        val embeddedLabelIndex = resDef.indexOf(oldFashionedLabel)
//        if (embeddedLabelIndex != -1) {
//            resDef = resDef.removeRange(embeddedLabelIndex, embeddedLabelIndex + oldFashionedLabel.length)
//            addLabel("old-fashioned")
//        }

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

    fun addAntonym(ant: String): WordTeacherWordBuilder {
        antonyms.add(ant)
        return this
    }

    fun setIsSynonymsBlock(isSynonimsBlock: Boolean) {
        this.isSynonymsBlock = isSynonimsBlock
    }

    fun setIsAntonymsBlock(isAntonymsBlock: Boolean) {
        this.isAntonymsBlock = isAntonymsBlock
    }

    fun addText(text: String): WordTeacherWordBuilder {
        if (isSynonymsBlock) {
            addSynonym(text)
        } else if (isAntonymsBlock) {
            addAntonym(text)
        } else {
            addDefinition(text)
        }
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
                imageUrl,
                labels.toList(),
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
        labels.clear()
        isSynonymsBlock = false
        isAntonymsBlock = false
    }

    fun clear() {
        word = ""
        transcriptions.clear()
        wordDefinitions.clear()
        sourceNames.clear()
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
                LinkedHashMap(wordDefinitions),
                sourceNames.toList()
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
    } catch (e: Throwable) {
        WordTeacherWord.PartOfSpeech.Undefined
    }
}


fun WordTeacherWord.PartOfSpeech.Companion.fromString(string: String?): WordTeacherWord.PartOfSpeech {
    val res = string?.lowercase()?.trimNonLetterNonDigit() ?: "null"
    labelToPartOfSpeechMap[res]?.let {
        return it
    }

    return when {
        res == "pronoun" -> WordTeacherWord.PartOfSpeech.Pronoun
        res == "preposition" -> WordTeacherWord.PartOfSpeech.Preposition
        res == "conjunction" ||
                res == "conj-subord" ||
                res == "phr-conj-subord" -> WordTeacherWord.PartOfSpeech.Conjunction
        res == "interjection" -> WordTeacherWord.PartOfSpeech.Interjection
        res == "abbreviation" -> WordTeacherWord.PartOfSpeech.Abbreviation
        res == "determiner" -> WordTeacherWord.PartOfSpeech.Determiner
        res == "exclamation" -> WordTeacherWord.PartOfSpeech.Exclamation
        res == "phrasal verb" -> WordTeacherWord.PartOfSpeech.PhrasalVerb
        res.contains("noun") -> WordTeacherWord.PartOfSpeech.Noun
        res.contains("verb") -> WordTeacherWord.PartOfSpeech.Verb
        res.contains("adverb") -> WordTeacherWord.PartOfSpeech.Adverb
        res.contains("adjective") -> WordTeacherWord.PartOfSpeech.Adjective
        else -> {
            if (string != null) {
                //Log.d("WordTeacherWord", "New Part of Speech has found: $string")
            }
            WordTeacherWord.PartOfSpeech.Undefined
        }
    }
}

fun WordTeacherWord.PartOfSpeech.toStringResource(): StringResourceWrapper {
    val res = when(this) {
        WordTeacherWord.PartOfSpeech.Noun -> Res.string.word_partofspeech_noun
        WordTeacherWord.PartOfSpeech.Verb -> Res.string.word_partofspeech_verb
        WordTeacherWord.PartOfSpeech.Adjective -> Res.string.word_partofspeech_adjective
        WordTeacherWord.PartOfSpeech.Adverb -> Res.string.word_partofspeech_adverb
        WordTeacherWord.PartOfSpeech.Pronoun -> Res.string.word_partofspeech_pronoun
        WordTeacherWord.PartOfSpeech.Preposition -> Res.string.word_partofspeech_preposition
        WordTeacherWord.PartOfSpeech.Conjunction -> Res.string.word_partofspeech_conjunction
        WordTeacherWord.PartOfSpeech.Interjection -> Res.string.word_partofspeech_interjection
        WordTeacherWord.PartOfSpeech.Abbreviation -> Res.string.word_partofspeech_abbreviation
        WordTeacherWord.PartOfSpeech.Exclamation -> Res.string.word_partofspeech_exclamation
        WordTeacherWord.PartOfSpeech.Determiner -> Res.string.word_partofspeech_determiner
        WordTeacherWord.PartOfSpeech.PhrasalVerb -> Res.string.word_partofspeech_phrasalVerb
        else -> Res.string.word_partofspeech_unknown
    }
    return StringResourceWrapper.String(res)
}
