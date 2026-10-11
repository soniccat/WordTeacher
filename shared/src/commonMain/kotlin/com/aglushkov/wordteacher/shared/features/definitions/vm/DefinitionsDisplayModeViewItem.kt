package com.aglushkov.wordteacher.shared.features.definitions.vm

import org.jetbrains.compose.resources.StringResource
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper

class DefinitionsDisplayModeViewItem(
    val partsOfSpeechFilterText: StringResourceWrapper,
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