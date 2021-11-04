package com.aglushkov.wordteacher.shared.features.notes.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Article
import dev.icerock.moko.resources.desc.StringDesc

class CreateNoteViewItem(
    val placeholder: StringDesc,
    val text: String = ""
): BaseViewItem<Unit>(Unit, Type, -1) {
    companion object {
        const val Type = 501
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        other as CreateNoteViewItem
        return super.equalsByContent(other)
    }
}
