package com.aglushkov.wordteacher.shared.features.definitions.vm

import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

class DefinitionsDisplayModeViewItem(
    val partsOfSpeechFilterText: StringDesc,
    val canClearPartsOfSpeechFilter: Boolean,
    modes: List<DefinitionsDisplayMode>,
    val selectedIndex: Int
): BaseViewItem<DefinitionsDisplayMode>(modes, Type) {

    companion object {
        const val Type = 200
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) &&
                partsOfSpeechFilterText == (other as DefinitionsDisplayModeViewItem).partsOfSpeechFilterText &&
                canClearPartsOfSpeechFilter == other.canClearPartsOfSpeechFilter &&
                selectedIndex == other.selectedIndex
    }
}