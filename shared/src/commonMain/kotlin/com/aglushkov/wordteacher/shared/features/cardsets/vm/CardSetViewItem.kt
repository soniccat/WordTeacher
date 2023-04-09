package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CardSetViewItem(
    override val id: Long,
    val name: String,
    val date: String,
    val readyToLearnProgress: Float = 0f,
    val totalProgress: Float = 0f,
    override val type: Int = 400,
    override val items: ImmutableList<String> = persistentListOf(name),
): BaseViewItem<String> {

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        other as CardSetViewItem
        return super.equalsByContent(other) && date == other.date
    }

    override fun copyWithId(id: Long) = this.copy(id = id)
}
