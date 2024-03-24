package com.aglushkov.wordteacher.shared.features.cardset_info.di

import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.ArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.di.ArticleModule
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.definitions.di.DefinitionsDependencies
import com.aglushkov.wordteacher.shared.features.definitions.di.DefinitionsModule
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import com.arkivanov.decompose.ComponentContext
import com.russhwolf.settings.coroutines.FlowSettings
import dagger.BindsInstance
import dagger.Component

@Component(
    dependencies = [CardSetInfoDependencies::class],
    modules = [CardSetInfoModule::class]
)
interface CardSetInfoComponent {
    fun cardSetInfoDecomposeComponent(): CardSetInfoDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance
        fun setInitialState(state: CardSetInfoVM.State): Builder

        fun setDeps(deps: CardSetInfoDependencies): Builder
        fun build(): CardSetInfoComponent
    }
}

interface CardSetInfoDependencies {
    fun databaseCardWorker(): DatabaseCardWorker
    fun databaseWorker(): DatabaseWorker
    fun timeSource(): TimeSource
}