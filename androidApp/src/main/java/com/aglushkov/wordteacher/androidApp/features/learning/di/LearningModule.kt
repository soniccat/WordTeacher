package com.aglushkov.wordteacher.androidApp.features.learning.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.learning.LearningDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.data_loader.CardLoader
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseWorker
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class LearningModule {

    @Provides
    fun learningDecomposeComponent(
        state: LearningVM.State,
        routerResolver: RouterResolver,
        componentContext: ComponentContext,
        cardLoader: CardLoader,
        database: AppDatabase,
        databaseWorker: DatabaseWorker,
        timeSource: TimeSource,
        idGenerator: IdGenerator
    ) = LearningDecomposeComponent(
        state,
        routerResolver.router!!.get()!!,
        componentContext,
        cardLoader,
        database,
        databaseWorker,
        timeSource,
        idGenerator,
    )
}