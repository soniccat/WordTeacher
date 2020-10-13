package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem


class DefinitionsDisplayModeViewItem(
    modes: List<DefinitionsDisplayMode>,
    val selected: DefinitionsDisplayMode
): BaseViewItem<DefinitionsDisplayMode>(modes, Type) {

    companion object {
        const val Type = 200
    }
}