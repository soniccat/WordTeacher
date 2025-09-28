package com.aglushkov.wordteacher.shared.features.definitions.vm

import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyLevelAndRatio


class WordViewItem(word: WordTeacherWord): BaseViewItem<WordTeacherWord>(word, Type) {
    companion object {
        const val Type = 100
    }
}

class WordTitleViewItem(title: String, val providers: List<Config.Type>, val cardId: Long = -1, val frequencyLevelAndRatio: WordFrequencyLevelAndRatio?): BaseViewItem<String>(title, Type) {
    companion object {
        const val Type = 101
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) && providers == (other as WordTitleViewItem).providers && frequencyLevelAndRatio == other.frequencyLevelAndRatio
    }
}

class WordTranscriptionViewItem(transcription: String, val cardId: Long = -1): BaseViewItem<String>(transcription, Type) {
    companion object {
        const val Type = 102
    }
}

class WordAudioFilesViewItem(
    audioFiles: List<AudioFile>,
): BaseViewItem<WordAudioFilesViewItem.AudioFile>(audioFiles, Type) {
    companion object {
        const val Type = 112
    }

    data class AudioFile(
        val url: String,
        val name: String
    )
}

class WordPartOfSpeechViewItem(
    partOfSpeechString: StringDesc,
    val partOfSpeech: WordTeacherWord.PartOfSpeech,
    val cardId: Long = -1
): BaseViewItem<StringDesc>(partOfSpeechString, Type) {
    companion object {
        const val Type = 103
    }
}

class WordDefinitionViewItem(
    definition: String,
    val data: Any? = null,
    val index: Int = -1,
    val isLast: Boolean = false,
    val cardId: Long = -1,
    val withAddButton: Boolean = false,
    val labels: List<String> = emptyList(),
    val showAddLabel: Boolean = false,
): BaseViewItem<String>(definition, Type) {
    companion object {
        const val Type = 104
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) &&
                other is WordDefinitionViewItem &&
                labels == other.labels
    }
}

class WordExampleViewItem(
    example: String,
    val indent: Indent = Indent.NONE,
    val index: Int = -1,
    val isLast: Boolean = false,
    val cardId: Long = -1,
): BaseViewItem<String>(example, Type) {
    companion object {
        const val Type = 105
    }
}

// TODO: consider to merge WordExampleViewItem and WordSynonymViewItem into sth more general like WordSubHeaderViewItem
// for example WordListValue
class WordSynonymViewItem(synonym: String, val indent: Indent = Indent.NONE, val index: Int = -1, val isLast: Boolean = false, val cardId: Long = -1): BaseViewItem<String>(synonym, Type) {
    companion object {
        const val Type = 106
    }
}

class WordHeaderViewItem(name: StringDesc): BaseViewItem<StringDesc>(name, Type) {
    companion object {
        const val Type = 107
    }
}

class WordSubHeaderViewItem(
    name: StringDesc,
    val indent: Indent = Indent.NONE,
    val isOnlyHeader: Boolean = false,
    val contentType: ContentType = ContentType.UNKNOWN,
    val cardId: Long = -1
): BaseViewItem<StringDesc>(name, Type) {
    companion object {
        const val Type = 108
    }

    enum class ContentType {
        UNKNOWN,
        SYNONYMS,
        EXAMPLES
    }
}

class WordDividerViewItem(): BaseViewItem<Unit>(Unit, Type) {
    companion object {
        const val Type = 109
    }
}

class WordLoadingViewItem: BaseViewItem<Unit>(Unit, Type) {
    companion object {
        const val Type = 110
    }
}

class WordHistoryViewItem(
    id: Long,
    word: String,
): BaseViewItem<String>(word, Type, id) {
    companion object {
        const val Type = 111
    }
}

enum class Indent {
    NONE,
    SMALL
}
