package com.aglushkov.wordteacher.shared.features.notes.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Article

class NoteViewItem(
    noteId: Long,
    val date: String,
    val text: String,
): BaseViewItem<String>(text, Type, noteId) {
    companion object {
        const val Type = 500
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        other as NoteViewItem
        return super.equalsByContent(other) && date == other.date
    }
}
