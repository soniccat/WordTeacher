package com.aglushkov.wordteacher.shared.features.cardset.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CreateCardViewItem(
    override val id: Long = 0L,
    override val type: Int = 700
): BaseViewItem<Unit> {
    override val items: ImmutableList<Unit> = persistentListOf()

    override fun copyWithId(id: Long): BaseViewItem<Unit> = this.copy(id = id)
}

//class CardViewItem(
//    val cardId: Long,
//    val innerViewItems: List<BaseViewItem<*>>
//): BaseViewItem<Long>(cardId, Type, cardId) {
//    companion object {
//        const val Type = 701
//    }
//}
