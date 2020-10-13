package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.repository.Config
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc

@Parcelize
data class WordTeacherWord(
    val word: String,
    val transcription: String?,
    val definitions: Map<PartOfSpeech, List<WordTeacherDefinition>>,
    val types: List<Config.Type>
) : Parcelable {

    enum class PartOfSpeech {
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
        Undefined;

        companion object
    }

    companion object
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
        else -> MR.strings.word_partofspeech_unknown
    }
    return StringDesc.Resource(res)
}