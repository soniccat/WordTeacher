package com.aglushkov.wordteacher.androidApp.features.learning.di

import com.aglushkov.wordteacher.androidApp.features.CardLoaderModule
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.learning.LearningDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(
    dependencies = [LearningDependencies::class],
    modules = [LearningModule::class, CardLoaderModule::class]
)
interface LearningComponent {
    fun learningDecomposeComponent(): LearningDecomposeComponent

    // TODO: switch to factory
    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setState(configuration: LearningVM.State): Builder

        fun setDeps(deps: LearningDependencies): Builder
        fun build(): LearningComponent
    }
}

interface LearningDependencies {
    fun database(): AppDatabase
    fun databaseWorker(): DatabaseWorker
    fun databaseCardSetWorker(): DatabaseCardWorker
    fun idGenerator(): IdGenerator
    fun timeSource(): TimeSource
    fun routerResolver(): RouterResolver
    fun nlpCore(): NLPCore
}
