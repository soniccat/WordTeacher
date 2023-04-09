package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class LearningInputViewItem(
    override val id: Long,
    override val type: Int = 801
): BaseViewItem<Unit> {
    override val items: ImmutableList<Unit> = persistentListOf()

    override fun copyWithId(id: Long): BaseViewItem<Unit> = this.copy(id = id)
}
