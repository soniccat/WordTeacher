package com.aglushkov.wordteacher.shared.general.views.chooser_dialog

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

class ChooserViewItem (
    anId: Long,
    val name: String,
    val obj: Any,
    var isSelected: Boolean
): BaseViewItem<String>(name, Type, anId) {
    companion object {
        const val Type = 301
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) && isSelected == (other as ChooserViewItem).isSelected
    }
}
