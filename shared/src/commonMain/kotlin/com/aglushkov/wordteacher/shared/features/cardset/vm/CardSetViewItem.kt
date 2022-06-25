package com.aglushkov.wordteacher.shared.features.cardset.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

class CreateCardViewItem(
): BaseViewItem<Unit>(Unit, Type, -1) {
    companion object {
        const val Type = 700
    }
}

//class CardViewItem(
//    val cardId: Long,
//    val innerViewItems: List<BaseViewItem<*>>
//): BaseViewItem<Long>(cardId, Type, cardId) {
//    companion object {
//        const val Type = 701
//    }
//}
