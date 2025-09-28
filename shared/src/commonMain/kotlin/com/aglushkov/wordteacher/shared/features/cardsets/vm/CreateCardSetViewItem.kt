package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Article
import dev.icerock.moko.resources.desc.StringDesc

class CreateCardSetViewItem(
    val placeholder: StringDesc,
    val text: String = ""
): BaseViewItem<Unit>(Unit, Type, -1) {
    companion object {
        const val Type = 601
    }
}
