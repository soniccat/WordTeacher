package com.aglushkov.wordteacher.androidApp.features.cardsets.di

import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.cardsets.CardSetsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class CardSetsComposeModule {

    @Provides
    fun viewModel(
        state: CardSetsVM.State,
        cardSetsRepository: CardSetsRepository,
        routerResolver: RouterResolver,
        componentContext: ComponentContext,
        timeSource: TimeSource
    ): CardSetsDecomposeComponent {
        return CardSetsDecomposeComponent(
            state,
            cardSetsRepository,
            routerResolver.router!!.get()!!,
            componentContext,
            timeSource
        )
    }
}