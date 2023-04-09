package com.aglushkov.wordteacher.shared.features.definitions.vm

import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import kotlinx.collections.immutable.ImmutableList

data class DefinitionsDisplayModeViewItem(
    val partsOfSpeechFilterText: StringDesc,
    val canClearPartsOfSpeechFilter: Boolean,
    override val items: ImmutableList<DefinitionsDisplayMode>,
    val selectedIndex: Int,
    override val id: Long = 0L,
    override val type: Int = 200,
): BaseViewItem<DefinitionsDisplayMode> {

    override fun copyWithId(id: Long): BaseViewItem<DefinitionsDisplayMode> = copy(id = id)

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) &&
                partsOfSpeechFilterText == (other as DefinitionsDisplayModeViewItem).partsOfSpeechFilterText &&
                canClearPartsOfSpeechFilter == other.canClearPartsOfSpeechFilter &&
                selectedIndex == other.selectedIndex
    }
}