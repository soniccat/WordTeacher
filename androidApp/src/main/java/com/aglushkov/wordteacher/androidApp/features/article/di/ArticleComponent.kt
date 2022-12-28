package com.aglushkov.wordteacher.androidApp.features.article.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DefinitionsComposeModule
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DefinitionsDependencies
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.ArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.arkivanov.decompose.ComponentContext
import com.russhwolf.settings.coroutines.FlowSettings
import dagger.BindsInstance
import dagger.Component

@FragmentComp
@Component(
    dependencies = [ArticleDependencies::class, DefinitionsDependencies::class],
    modules = [ArticleModule::class, DefinitionsComposeModule::class]
)
interface ArticleComposeComponent {
    fun articleDecomposeComponent(): ArticleDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setConfiguration(configuration: MainDecomposeComponent.ChildConfiguration.ArticleConfiguration): Builder

        fun setDeps(deps: ArticleDependencies): Builder
        fun setDefinitionsDeps(deps: DefinitionsDependencies): Builder
        fun build(): ArticleComposeComponent
    }
}

interface ArticleDependencies {
    fun database(): AppDatabase
    fun nlpCore(): NLPCore
    fun settings(): FlowSettings
}