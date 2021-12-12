package com.aglushkov.wordteacher.shared.features.definitions.vm

import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config


class WordViewItem(word: WordTeacherWord): BaseViewItem<WordTeacherWord>(word, Type) {
    companion object {
        const val Type = 100
    }
}

class WordTitleViewItem(title: String, val providers: List<Config.Type>): BaseViewItem<String>(title, Type) {
    companion object {
        const val Type = 101
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) && providers == (other as WordTitleViewItem).providers
    }
}

class WordTranscriptionViewItem(transcription: String): BaseViewItem<String>(transcription, Type) {
    companion object {
        const val Type = 102
    }
}

class WordPartOfSpeechViewItem(partOfSpeech: StringDesc): BaseViewItem<StringDesc>(partOfSpeech, Type) {
    companion object {
        const val Type = 103
    }
}

class WordDefinitionViewItem(definition: String, val index: Int = -1): BaseViewItem<String>(definition, Type) {
    companion object {
        const val Type = 104
    }
}

class WordExampleViewItem(example: String, val indent: Indent = Indent.NONE, val index: Int = -1): BaseViewItem<String>(example, Type) {
    companion object {
        const val Type = 105
    }
}

class WordSynonymViewItem(synonym: String, val indent: Indent = Indent.NONE, val index: Int = -1): BaseViewItem<String>(synonym, Type) {
    companion object {
        const val Type = 106
    }
}

class WordHeaderViewItem(name: StringDesc): BaseViewItem<StringDesc>(name, Type) {
    companion object {
        const val Type = 107
    }
}

class WordSubHeaderViewItem(name: StringDesc, val indent: Indent = Indent.NONE): BaseViewItem<StringDesc>(name, Type) {
    companion object {
        const val Type = 108
    }
}

class WordDividerViewItem(): BaseViewItem<Any>(Obj, Type) {
    companion object {
        val Obj = Any()
        const val Type = 109
    }
}

enum class Indent {
    NONE,
    SMALL
}