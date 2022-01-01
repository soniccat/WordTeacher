package com.aglushkov.wordteacher.shared.features.cardset.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.MutableCard

class CreateCardViewItem(
): BaseViewItem<Unit>(Unit, Type, -1) {
    companion object {
        const val Type = 700
    }
}

class CardViewItem(
    val card: MutableCard,
    val innerViewItems: List<BaseViewItem<*>>
): BaseViewItem<Card>(card, Type, card.id) {
    companion object {
        const val Type = 701
    }
}
