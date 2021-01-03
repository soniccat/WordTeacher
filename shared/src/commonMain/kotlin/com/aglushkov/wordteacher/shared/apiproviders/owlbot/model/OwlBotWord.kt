package com.aglushkov.wordteacher.shared.apiproviders.owlbot.model

import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.fromString
import com.aglushkov.wordteacher.shared.repository.config.Config
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class OwlBotWord(
    @SerialName("definitions") val definitions: List<OwlBotDefinition>,
    @SerialName("pronunciation") val pronunciation: String?,
    @SerialName("word") val word: String?
) : Parcelable

fun OwlBotWord.asWordTeacherWord(): WordTeacherWord? {
    if (word == null) return null

    val map: MutableMap<WordTeacherWord.PartOfSpeech, List<WordTeacherDefinition>> = mutableMapOf()
    for (definition in definitions) {
        val partOfSpeech = WordTeacherWord.PartOfSpeech.fromString(definition.type)
        definition.asWordTeacherDefinition()?.let {
            map[partOfSpeech] = listOf(it)
        }
    }

    return WordTeacherWord(word,
        pronunciation,
        map,
        listOf(Config.Type.OwlBot))
}