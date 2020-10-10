package com.aglushkov.wordteacher.apiproviders.google.model

import com.aglushkov.wordteacher.shared.wordteacher.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.wordteacher.repository.Config
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName


@Parcelize
data class GoogleWord(
    @SerialName("meaning") val definitions: GoogleDefinitions,
    @SerialName("origin") val origin: String?,
    @SerialName("phonetic") val phonetic: String?,
    @SerialName("word") val word: String
) : Parcelable

fun GoogleWord.asWordTeacherWord(): WordTeacherWord? {
    return WordTeacherWord(word,
            phonetic,
            definitions.asMap(),
            listOf(Config.Type.Google))
}