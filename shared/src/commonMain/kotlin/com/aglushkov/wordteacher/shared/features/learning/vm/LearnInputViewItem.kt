package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

class LearnInputViewItem(
    val cardId: Long
): BaseViewItem<String>("", Type, cardId) {
    companion object {
        const val Type = 800
    }
}
