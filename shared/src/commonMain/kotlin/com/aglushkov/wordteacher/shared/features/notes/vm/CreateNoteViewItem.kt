package com.aglushkov.wordteacher.shared.features.notes.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Article
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CreateNoteViewItem(
    override val id: Long = 0L,
    override val type: Int = 501,
    val placeholder: StringDesc,
    val text: String = ""
): BaseViewItem<Unit> {
    override val items: ImmutableList<Unit> = persistentListOf()

    override fun copyWithId(id: Long): BaseViewItem<Unit> = this.copy(id = id)

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        other as CreateNoteViewItem
        return super.equalsByContent(other)
    }
}
