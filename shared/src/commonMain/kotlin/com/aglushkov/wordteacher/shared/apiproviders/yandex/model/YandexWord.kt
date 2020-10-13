package com.aglushkov.wordteacher.apiproviders.yandex.model

import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.fromString
import com.aglushkov.wordteacher.shared.repository.Config
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class YandexWord(
    @SerialName("tr") val definitions: List<YandexDefinition>,
    @SerialName("ts") val transcription: String? = null,

    // Universal attributes
    @SerialName("text") val text: String,
    @SerialName("num") val num: String? = null,
    @SerialName("pos") val pos: String? = null,
    @SerialName("gen") val gender: String? = null,
    @SerialName("asp") val asp: String? = null
) : Parcelable

fun YandexWord.asWordTeacherWord(): WordTeacherWord? {
    val map: MutableMap<WordTeacherWord.PartOfSpeech, List<WordTeacherDefinition>> = mutableMapOf()
    for (definition in definitions) {
        val partOfSpeech = WordTeacherWord.PartOfSpeech.fromString(definition.pos)
        definition.asWordTeacherDefinition()?.let {
            var list = map[partOfSpeech] as? MutableList<WordTeacherDefinition>
            if (list == null) {
                list = mutableListOf()
                map[partOfSpeech] = list
            }

            list.add(it)
        }
    }

    return WordTeacherWord(text,
            transcription,
            map,
            listOf(Config.Type.Yandex))
}