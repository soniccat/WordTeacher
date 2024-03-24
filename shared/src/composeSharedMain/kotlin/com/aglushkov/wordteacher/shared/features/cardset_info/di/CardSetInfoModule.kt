package com.aglushkov.wordteacher.shared.features.cardset_info.di

import com.aglushkov.wordteacher.shared.features.cardset.CardSetDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class CardSetInfoModule {

    @Provides
    fun cardSetRepository(
        databaseWorker: DatabaseWorker,
        timeSource: TimeSource
    ) = CardSetRepository(databaseWorker, timeSource)

    @Provides
    fun cardSetInfoDecomposeComponent(
        componentContext: ComponentContext,
        initState: CardSetInfoVM.State,
        databaseCardWorker: DatabaseCardWorker,
        cardSetRepository: CardSetRepository,
    ) = CardSetInfoDecomposeComponent(
        componentContext,
        initState,
        databaseCardWorker,
        cardSetRepository,
    )
}
