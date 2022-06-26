package com.aglushkov.wordteacher.shared.events

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

open class FocusViewItemEvent(
    val viewItem: BaseViewItem<*>,
    override var isHandled: Boolean = false
): Event {
    override fun markAsHandled() {
        isHandled = true
    }
}