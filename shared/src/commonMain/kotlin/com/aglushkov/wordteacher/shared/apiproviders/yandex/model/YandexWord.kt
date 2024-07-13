package com.aglushkov.wordteacher.shared.apiproviders.yandex.model

import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.fromString
import com.aglushkov.wordteacher.shared.repository.config.Config
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
)

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
            transcription?.let { listOf(it) } ?: emptyList(),
            map,
            listOf(Config.Type.Yandex))
}