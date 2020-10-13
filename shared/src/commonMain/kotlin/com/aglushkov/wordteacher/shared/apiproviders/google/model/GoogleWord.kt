package com.aglushkov.wordteacher.apiproviders.google.model

import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.Config
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class GoogleWord(
    @SerialName("meaning") val definitions: GoogleDefinitions,
    @SerialName("origin") val origin: String? = null,
    @SerialName("phonetic") val phonetic: String? = null,
    @SerialName("word") val word: String
) : Parcelable

fun GoogleWord.asWordTeacherWord(): WordTeacherWord? {
    return WordTeacherWord(word,
            phonetic,
            definitions.asMap(),
            listOf(Config.Type.Google))
}