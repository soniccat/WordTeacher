package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.WordTeacherWord

class DefinitionsDisplayModeViewItem(
    val partsOfSpeechFilter: List<WordTeacherWord.PartOfSpeech>,
    modes: List<DefinitionsDisplayMode>,
    val selectedIndex: Int
): BaseViewItem<DefinitionsDisplayMode>(modes, Type) {

    companion object {
        const val Type = 200
    }
}