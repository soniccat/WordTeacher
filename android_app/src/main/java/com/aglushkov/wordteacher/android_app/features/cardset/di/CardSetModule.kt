package com.aglushkov.wordteacher.android_app.features.cardset.di

import com.aglushkov.wordteacher.android_app.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.cardset.CardSetDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class CardSetModule {

    @Provides
    fun cardSetRepository(
        database: AppDatabase,
        databaseWorker: DatabaseWorker,
        timeSource: TimeSource
    ) = CardSetRepository(database, databaseWorker, timeSource)

    @Provides
    fun cardSetDecomposeComponent(
        state: CardSetVM.State,
        routerResolver: RouterResolver,
        notesRepository: CardSetRepository,
        databaseCardWorker: DatabaseCardWorker,
        componentContext: ComponentContext,
        timeSource: TimeSource,
        idGenerator: IdGenerator
    ) = CardSetDecomposeComponent(
        state,
        routerResolver.router!!.get()!!,
        notesRepository,
        databaseCardWorker,
        componentContext,
        timeSource,
        idGenerator
    )
}
