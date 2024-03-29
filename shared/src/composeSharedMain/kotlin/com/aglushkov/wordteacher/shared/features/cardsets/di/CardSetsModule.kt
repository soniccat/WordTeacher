package com.aglushkov.wordteacher.shared.features.cardsets.di

import com.aglushkov.wordteacher.shared.features.cardsets.CardSetsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.cardsetsearch.CardSetSearchRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class CardSetsModule {

    @Provides
    fun viewModel(
        state: CardSetsVM.State,
        cardSetsRepository: CardSetsRepository,
        cardSetSearchRepository: CardSetSearchRepository,
        componentContext: ComponentContext,
        timeSource: TimeSource,
        idGenerator: IdGenerator,
        features: CardSetsVM.Features,
    ): CardSetsDecomposeComponent {
        return CardSetsDecomposeComponent(
            state,
            cardSetsRepository,
            cardSetSearchRepository,
            componentContext,
            timeSource,
            idGenerator,
            features
        )
    }
}