package com.aglushkov.wordteacher.apiproviders.wordnik.model

import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.fromString
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.arkivanov.decompose.statekeeper.Parcelable
import com.arkivanov.decompose.statekeeper.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: check possible values of WordnikLinkRelatedWords.relationshipType
@Parcelize
@Serializable
data class WordnikWord(
        @SerialName("id") val id: String? = null,
        @SerialName("attributionText") val attributionText: String? = null,
        @SerialName("attributionUrl") val attributionUrl: String? = null,
        @SerialName("citations") val citations: List<WordnikCitation>,
        @SerialName("exampleUses") val exampleUses: List<WordnikExampleUse>,
        @SerialName("labels") val labels: List<WordnikLabel>,
//        @SerializedName("notes") val notes: List<Any>,
        @SerialName("partOfSpeech") val partOfSpeech: String? = null,
        @SerialName("relatedWords") val relatedWords: List<WordnikRelatedWords>,
        @SerialName("sourceDictionary") val sourceDictionary: String? = null,
        @SerialName("text") val text: String? = null,
//        @SerializedName("textProns") val textProns: List<Any>,
        @SerialName("word") val word: String? = null,
        @SerialName("wordnikUrl") val wordnikUrl: String? = null
) : Parcelable {
    fun exampleUsesTexts() = exampleUses.mapNotNull { it.text } + (citations.mapNotNull { it.cite })
    fun synonyms() = relatedWords.filter { it.relationshipType == "synonym" }.map { it.words }.flatten()
    fun related() = relatedWords.filter { it.relationshipType != "synonym" }.map { it.words }.flatten()
}

fun List<WordnikWord>.asWordTeacherWords(): List<WordTeacherWord> {
    val map: MutableMap<String, WordTeacherWord> = mutableMapOf()

    for (word in this) {
        if (word.word == null) continue

        val partOfSpeech = WordTeacherWord.PartOfSpeech.fromString(word.partOfSpeech)
        val definition = word.asDefinition() ?: continue
        val wordTeacherWord = map[word.word] ?: run {
            val resultWord = WordTeacherWord(word.word,
                    null,
                    mutableMapOf(partOfSpeech to mutableListOf()),
                    listOf(Config.Type.Wordnik))
            map[word.word] = resultWord
            resultWord
        }

        val definitionsMap = wordTeacherWord.definitions as MutableMap
        val definitionsList = definitionsMap[partOfSpeech] as? MutableList ?: run {
            val def = mutableListOf<WordTeacherDefinition>()
            definitionsMap[partOfSpeech] = def
            def
        }

        definitionsList.add(definition)
    }

    return map.values.toList()
}

fun WordnikWord.asDefinition(): WordTeacherDefinition? {
    if (text == null) return null

    return WordTeacherDefinition(
            listOf(text),
            exampleUsesTexts(),
            synonyms(),
            null
    )
}