package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Article
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CreateCardSetViewItem(
    override val id: Long = 0L,
    override val type: Int = 601,
    val placeholder: StringDesc,
    val text: String = ""
): BaseViewItem<Unit> {
    override val items: ImmutableList<Unit> = persistentListOf()

    override fun copyWithId(id: Long): BaseViewItem<Unit> = copy(id = id)
}
