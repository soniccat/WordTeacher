package com.aglushkov.wordteacher.shared.features.cardsets

import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsRouter
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.cardsetsearch.CardSetSearchRepository
import com.arkivanov.decompose.ComponentContext

class CardSetsDecomposeComponent (
    initialState: CardSetsVM.State,
    cardSetsRepository: CardSetsRepository,
    cardSetSearchRepository: CardSetSearchRepository,
    componentContext: ComponentContext,
    timeSource: TimeSource,
    idGenerator: IdGenerator,
    features: CardSetsVM.Features
) : CardSetsVMImpl(
    initialState,
    cardSetsRepository,
    cardSetSearchRepository,
    timeSource,
    idGenerator,
    features
), ComponentContext by componentContext {
}
