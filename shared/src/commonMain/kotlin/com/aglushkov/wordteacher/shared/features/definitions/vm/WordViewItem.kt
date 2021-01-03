package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import dev.icerock.moko.resources.desc.StringDesc


class WordViewItem(word: WordTeacherWord): BaseViewItem<WordTeacherWord>(word, Type) {
    companion object {
        const val Type = 100
    }
}

class WordTitleViewItem(title: String, val providers: List<Config.Type>): BaseViewItem<String>(title, Type) {
    companion object {
        const val Type = 101
    }

    override fun equalsByIds(item: BaseViewItem<*>): Boolean {
        return super.equalsByIds(item) && item is WordTitleViewItem && providers == item.providers
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

class WordDefinitionViewItem(definition: String): BaseViewItem<String>(definition, Type) {
    companion object {
        const val Type = 104
    }
}

class WordExampleViewItem(example: String): BaseViewItem<String>(example, Type) {
    companion object {
        const val Type = 105
    }
}

class WordSynonymViewItem(synonym: String): BaseViewItem<String>(synonym, Type) {
    companion object {
        const val Type = 106
    }
}

class WordSubHeaderViewItem(name: StringDesc): BaseViewItem<StringDesc>(name, Type) {
    companion object {
        const val Type = 107
    }
}

class WordDividerViewItem(): BaseViewItem<Any>(Obj, Type) {
    companion object {
        val Obj = Any()
        const val Type = 108
    }
}