package com.aglushkov.wordteacher.android_app.general.views.chooser_dialog

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ChooserViewItem (
    override val id: Long,
    val name: String,
    val obj: Any,
    var isSelected: Boolean,
    override val type: Int = 301,
): BaseViewItem<String> {
    override val items: ImmutableList<String> = persistentListOf(name)

    override fun copyWithId(id: Long): BaseViewItem<String> = this.copy(id = id)

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) && isSelected == (other as ChooserViewItem).isSelected
    }
}
