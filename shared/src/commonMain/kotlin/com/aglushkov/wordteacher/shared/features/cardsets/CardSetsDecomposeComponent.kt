package com.aglushkov.wordteacher.shared.features.cardsets

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsRouter
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.cardsetsearch.CardSetSearchRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy

class CardSetsDecomposeComponent (
    initialState: CardSetsVM.State,
    cardSetsRepository: CardSetsRepository,
    cardSetSearchRepository: CardSetSearchRepository,
    componentContext: ComponentContext,
    timeSource: TimeSource,
    idGenerator: IdGenerator,
    features: CardSetsVM.Features,
    analytics: Analytics,
) : CardSetsVMImpl(
    componentContext.stateKeeper.consume(
        key = KEY_STATE,
        strategy = CardSetsVM.State.serializer()
    ) ?: initialState,
    cardSetsRepository,
    cardSetSearchRepository,
    timeSource,
    idGenerator,
    features
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_CardSets"

    init {
        baseInit(analytics)

        stateKeeper.register(
            key = KEY_STATE,
            strategy = CardSetsVM.State.serializer()
        ) { this.state }
    }

    private companion object {
        private const val KEY_STATE = "SAVED_STATE"
    }
}
