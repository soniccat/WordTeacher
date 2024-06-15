package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM

interface CardSetsRouter {
    fun openCardSet(state: CardSetVM.State)
    fun openLearning(ids: List<Long>)
    fun openJsonImport()
}