package com.aglushkov.wordteacher.androidApp.features.learning_session_result.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.learning.LearningDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.features.learning_session_result.LearningSessionResultDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.data_loader.CardLoader
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseWorker
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class LearningSessionResultModule {

    @Provides
    fun learningDecomposeComponent(
        state: LearningSessionResultVM.State,
        componentContext: ComponentContext,
        routerResolver: RouterResolver,
        cardLoader: CardLoader,
        idGenerator: IdGenerator
    ) = LearningSessionResultDecomposeComponent(
        state,
        routerResolver.router!!.get()!!,
        componentContext,
        cardLoader,
        idGenerator,
    )
}
