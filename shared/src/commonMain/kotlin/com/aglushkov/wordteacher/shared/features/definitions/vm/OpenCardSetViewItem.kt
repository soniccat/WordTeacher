package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Article
import dev.icerock.moko.resources.desc.StringDesc

class OpenCardSetViewItem(
    val text: StringDesc
): BaseViewItem<Unit>(Unit, Type, -1) {
    companion object {
        const val Type = 120
    }
}

