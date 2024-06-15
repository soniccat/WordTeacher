package com.aglushkov.wordteacher.shared.features.cardset.di

import com.aglushkov.wordteacher.shared.features.cardset.CardSetDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.service.SpaceCardSetService
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class CardSetModule {

    @Provides
    fun cardSetRepository(
        cardSetService: SpaceCardSetService,
        databaseWorker: DatabaseWorker,
        timeSource: TimeSource
    ) = CardSetRepository(cardSetService, databaseWorker, timeSource)

    @Provides
    fun cardSetDecomposeComponent(
        state: CardSetVM.State,
        cardSetsRepository: CardSetsRepository,
        cardSetRepository: CardSetRepository,
        wordFrequencyGradationProvider: WordFrequencyGradationProvider,
        databaseCardWorker: DatabaseCardWorker,
        componentContext: ComponentContext,
        timeSource: TimeSource,
        idGenerator: IdGenerator
    ) = CardSetDecomposeComponent(
        state,
        cardSetsRepository,
        cardSetRepository,
        wordFrequencyGradationProvider,
        databaseCardWorker,
        componentContext,
        timeSource,
        idGenerator
    )
}
