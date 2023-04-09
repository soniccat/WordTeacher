package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Article
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class OpenCardSetViewItem(
    override val id: Long = 0L,
    override val type: Int = 120,
    override val items: ImmutableList<Unit> = persistentListOf(),
    val text: StringDesc
): BaseViewItem<Unit> {

    override fun copyWithId(id: Long): BaseViewItem<Unit> = this.copy(id = id)

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        other as OpenCardSetViewItem
        return super.equalsByContent(other)
    }
}
