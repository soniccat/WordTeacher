package com.aglushkov.wordteacher.apiproviders.wordnik.model


import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.fromString
import com.aglushkov.wordteacher.shared.repository.Config
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.SerialName

// TODO: check possible values of WordnikLinkRelatedWords.relationshipType
@Parcelize
data class WordnikWord(
        @SerialName("id") val id: String?,
        @SerialName("attributionText") val attributionText: String?,
        @SerialName("attributionUrl") val attributionUrl: String?,
        @SerialName("citations") val citations: List<WordnikCitation>,
        @SerialName("exampleUses") val exampleUses: List<WordnikExampleUse>,
        @SerialName("labels") val labels: List<WordnikLabel>,
//        @SerializedName("notes") val notes: List<Any>,
        @SerialName("partOfSpeech") val partOfSpeech: String?,
        @SerialName("relatedWords") val relatedWords: List<WordnikRelatedWords>,
        @SerialName("sourceDictionary") val sourceDictionary: String?,
        @SerialName("text") val text: String?,
//        @SerializedName("textProns") val textProns: List<Any>,
        @SerialName("word") val word: String?,
        @SerialName("wordnikUrl") val wordnikUrl: String?
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
            val word = WordTeacherWord(word.word,
                    null,
                    mutableMapOf(partOfSpeech to mutableListOf()),
                    listOf(Config.Type.Wordnik))
            map[word.word] = word
            word
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