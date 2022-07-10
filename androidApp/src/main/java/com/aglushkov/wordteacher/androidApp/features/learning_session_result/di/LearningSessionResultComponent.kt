package com.aglushkov.wordteacher.androidApp.features.learning_session_result.di

import com.aglushkov.wordteacher.androidApp.features.CardLoaderModule
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.learning_session_result.LearningSessionResultDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(
    dependencies = [LearningSessionResultDependencies::class],
    modules = [LearningSessionResultModule::class, CardLoaderModule::class]
)
interface LearningSessionResultComponent {
    fun learningSessionResultDecomposeComponent(): LearningSessionResultDecomposeComponent

    // TODO: switch to factory
    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setState(configuration: LearningSessionResultVM.State): Builder

        fun setDeps(deps: LearningSessionResultDependencies): Builder
        fun build(): LearningSessionResultComponent
    }
}

interface LearningSessionResultDependencies {
    fun database(): AppDatabase
    fun databaseWorker(): DatabaseWorker
    fun idGenerator(): IdGenerator
    fun timeSource(): TimeSource
    fun routerResolver(): RouterResolver
}
