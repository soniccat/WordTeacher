package com.aglushkov.wordteacher.apiproviders.google.model

import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.arkivanov.decompose.statekeeper.Parcelable
import com.arkivanov.decompose.statekeeper.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: add "phonetics" support
/*
    {
        "word": "cat",
        "phonetics": [
            {
                "text": "/kæt/",
                "audio": "https://lex-audio.useremarkable.com/mp3/cat_us_1.mp3"
            }
        ],
        "meaning": {}
    }
 */
@Parcelize
@Serializable
data class GoogleWord(
    @SerialName("meaning") val definitions: GoogleDefinitions,
    @SerialName("origin") val origin: String? = null,
    @SerialName("phonetic") val phonetic: String? = null,
    @SerialName("word") val word: String
) : Parcelable

fun GoogleWord.asWordTeacherWord(): WordTeacherWord? {
    val definitionsMap = definitions.asMap()
    val isEmpty = phonetic == null && definitionsMap.isEmpty()

    return if (isEmpty) {
        null
    } else {
        WordTeacherWord(word,
            phonetic,
            definitionsMap,
            listOf(Config.Type.Google))
    }
}