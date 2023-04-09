package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class LearningLoadingViewItem(
    override val id: Long = 0L,
    override val type: Int = 800,
    override val items: ImmutableList<String> = persistentListOf(),
): BaseViewItem<String> {

    override fun copyWithId(id: Long): BaseViewItem<String> = copyWithId(id = id)
}

private const val Type = 800
