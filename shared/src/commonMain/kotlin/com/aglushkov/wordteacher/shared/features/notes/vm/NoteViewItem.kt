package com.aglushkov.wordteacher.shared.features.notes.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Article
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class NoteViewItem(
    override val id: Long,
    val date: String,
    var text: String,
    override val type: Int = 500,
): BaseViewItem<String> {
    override val items: ImmutableList<String> = persistentListOf(text)

    override fun copyWithId(id: Long): BaseViewItem<String> = this.copy(id = id)

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        other as NoteViewItem
        return super.equalsByContent(other) && date == other.date
    }
}
