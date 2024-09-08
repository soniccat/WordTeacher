package com.aglushkov.wordteacher.shared.events

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

open class FocusViewItemEvent(
    val viewItem: BaseViewItem<*>,
    val elementIndex: Int = 0,
    override var isHandled: Boolean = false
): Event {
    override fun markAsHandled() {
        isHandled = true
    }
}

open class ScrollViewItemEvent(
    val viewItem: BaseViewItem<*>,
    override var isHandled: Boolean = false
): Event {
    override fun markAsHandled() {
        isHandled = true
    }
}