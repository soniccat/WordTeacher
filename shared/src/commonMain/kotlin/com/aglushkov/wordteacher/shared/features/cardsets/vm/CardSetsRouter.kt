package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import dev.icerock.moko.resources.desc.StringDesc

interface CardSetsRouter {
    fun openCardSet(state: CardSetVM.State)
    fun openLearning(state: LearningVM.State)
    fun openJsonImport()

    fun onError(text: StringDesc)
    fun onCardSetCreated(cardSetId: Long, name: String)
    fun onCardSetLoadingError(remoteId: String, name: String, onActionCalled: (() -> Unit)?)
}