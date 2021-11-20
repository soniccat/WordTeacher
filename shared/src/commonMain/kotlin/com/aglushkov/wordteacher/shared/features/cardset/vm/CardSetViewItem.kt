package com.aglushkov.wordteacher.shared.features.cardset.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

class CreateCardViewItem(
): BaseViewItem<Unit>(Unit, Type, -1) {
    companion object {
        const val Type = 700
    }
}

class CardViewItem(
): BaseViewItem<Unit>(Unit, Type, -1) {
    companion object {
        const val Type = 701
    }
}

