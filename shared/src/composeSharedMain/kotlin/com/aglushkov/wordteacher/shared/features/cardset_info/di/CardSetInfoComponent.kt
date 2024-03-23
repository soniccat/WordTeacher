package com.aglushkov.wordteacher.shared.features.cardset_info.di

import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.ArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.di.ArticleModule
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.di.DefinitionsDependencies
import com.aglushkov.wordteacher.shared.features.definitions.di.DefinitionsModule
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
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
        fun setConfiguration(configuration: MainDecomposeComponent.ChildConfiguration.ArticleConfiguration): Builder

        fun setDeps(deps: CardSetInfoDependencies): Builder
        fun setDefinitionsDeps(deps: DefinitionsDependencies): Builder
        fun build(): CardSetInfoComponent
    }
}

interface CardSetInfoDependencies {
    fun database(): AppDatabase
    fun nlpCore(): NLPCore
    fun settings(): FlowSettings
}