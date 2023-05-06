package com.aglushkov.wordteacher.android_app.features.cardsets.di

import com.aglushkov.wordteacher.android_app.general.RouterResolver
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
        routerResolver: RouterResolver,
        componentContext: ComponentContext,
        timeSource: TimeSource,
        idGenerator: IdGenerator,
    ): CardSetsDecomposeComponent {
        return CardSetsDecomposeComponent(
            state,
            cardSetsRepository,
            cardSetSearchRepository,
            routerResolver.router!!.get()!!, 
            componentContext,
            timeSource,
            idGenerator
        )
    }
}