package com.aglushkov.wordteacher.shared.features.definitions.vm

import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf


data class WordViewItem(
    val word: WordTeacherWord,
    override val items: ImmutableList<WordTeacherWord> = persistentListOf(word),
    override val id: Long = 0L,
    override val type: Int = 100,
): BaseViewItem<WordTeacherWord> {
    override fun copyWithId(id: Long): BaseViewItem<WordTeacherWord> = copy(id = id)
}

data class WordTitleViewItem(
    val title: String,
    val providers: ImmutableList<Config.Type>,
    val cardId: Long = -1,
    override val id: Long = 0L,
    override val type: Int = 101,
    override val items: ImmutableList<String> = persistentListOf(title),
): BaseViewItem<String> {
    override fun copyWithId(id: Long): BaseViewItem<String> = copy(id = id)

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) && providers == (other as WordTitleViewItem).providers
    }
}

data class WordTranscriptionViewItem(
    val transcription: String,
    override val items: ImmutableList<String> = persistentListOf(transcription),
    val cardId: Long = -1,
    override val id: Long = 0L,
    override val type: Int = 102,
): BaseViewItem<String> {
    override fun copyWithId(id: Long): BaseViewItem<String> = copy(id = id)
}

data class WordPartOfSpeechViewItem(
    val partOfSpeechString: StringDesc,
    val partOfSpeech: WordTeacherWord.PartOfSpeech,
    val cardId: Long = -1,
    override val id: Long = 0L,
    override val type: Int = 103,
    override val items: ImmutableList<StringDesc> = persistentListOf(partOfSpeechString),
): BaseViewItem<StringDesc> {
    override fun copyWithId(id: Long): BaseViewItem<StringDesc> = copy(id = id)
}

data class WordDefinitionViewItem(
    val definition: String,
    override val items: ImmutableList<String> = persistentListOf(definition),
    val dataKey: String? = null,
    val index: Int = -1,
    val isLast: Boolean = false,
    val cardId: Long = -1,
    override val id: Long = 0L,
    override val type: Int = 104,
): BaseViewItem<String> {
    override fun copyWithId(id: Long): BaseViewItem<String> = copy(id = id)
}

data class WordExampleViewItem(
    val example: String,
    val indent: Indent = Indent.NONE,
    val index: Int = -1,
    val isLast: Boolean = false,
    val cardId: Long = -1,
    override val id: Long = 0L,
    override val type: Int = 105,
    override val items: ImmutableList<String> = persistentListOf(example),
): BaseViewItem<String> {
    override fun copyWithId(id: Long): BaseViewItem<String> = copy(id = id)
}

// TODO: consider to merge WordExampleViewItem and WordSynonymViewItem into sth more general like WordSubHeaderViewItem
// for example WordListValue
data class WordSynonymViewItem(
    val synonym: String,
    val indent: Indent = Indent.NONE,
    val index: Int = -1,
    val isLast: Boolean = false,
    val cardId: Long = -1,
    override val id: Long = 0L,
    override val type: Int = 106,
    override val items: ImmutableList<String> = persistentListOf(synonym),
): BaseViewItem<String> {
    override fun copyWithId(id: Long): BaseViewItem<String> = copy(id = id)
}

data class WordHeaderViewItem(
    val name: StringDesc,
    override val items: ImmutableList<StringDesc> = persistentListOf(name),
    override val id: Long = 0L,
    override val type: Int = 107,
): BaseViewItem<StringDesc> {
    override fun copyWithId(id: Long): BaseViewItem<StringDesc> = copy(id = id)
}

data class WordSubHeaderViewItem(
    val name: StringDesc,
    val indent: Indent = Indent.NONE,
    val isOnlyHeader: Boolean = false,
    val contentType: ContentType = ContentType.UNKNOWN,
    val cardId: Long = -1,
    override val id: Long = 0L,
    override val type: Int = 108,
    override val items: ImmutableList<StringDesc> = persistentListOf(name),
): BaseViewItem<StringDesc> {
    override fun copyWithId(id: Long): BaseViewItem<StringDesc> = copy(id = id)

    enum class ContentType {
        UNKNOWN,
        SYNONYMS,
        EXAMPLES
    }
}

class WordDividerViewItem(
    override val items: ImmutableList<Unit> = persistentListOf(),
    override val id: Long = 0L,
    override val type: Int = 109,
): BaseViewItem<Unit> {
    override fun copyWithId(id: Long): BaseViewItem<Unit> = WordDividerViewItem(
        id = id,
        type = type,
        items = items
    )
}

enum class Indent {
    NONE,
    SMALL
}
