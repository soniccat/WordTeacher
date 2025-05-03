package com.aglushkov.wordteacher.shared.features.cardset.vm

import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM

interface CardSetRouter {
    fun openLearning(state: LearningVM.State)
    fun closeCardSet()
    fun openCardSetInfo(state: CardSetInfoVM.State)
}