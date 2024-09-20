package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM

interface DefinitionsRouter {
    fun openCardSets()
    fun openCardSet(state: CardSetVM.State)
}