package com.aglushkov.wordteacher.shared.features.cardset.vm

import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM

interface CardSetRouter {
    fun openLearning(ids: List<Long>)
    fun closeCardSet()
    fun openCardSetInfo(state: CardSetInfoVM.State)
}