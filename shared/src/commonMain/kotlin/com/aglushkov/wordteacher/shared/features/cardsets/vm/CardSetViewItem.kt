package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

class CardSetViewItem(
    setId: Long,
    val name: String,
    val date: String,
    val readyToLearnProgress: Float = 0f,
    val totalProgress: Float = 0f
): BaseViewItem<String>(name, Type, setId) {
    companion object {
        const val Type = 400
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        other as CardSetViewItem
        return super.equalsByContent(other) && date == other.date
    }
}
