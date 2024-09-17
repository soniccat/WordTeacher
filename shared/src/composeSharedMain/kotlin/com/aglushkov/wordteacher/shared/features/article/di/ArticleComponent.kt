package com.aglushkov.wordteacher.shared.features.article.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.ArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.definitions.di.DefinitionsDependencies
import com.aglushkov.wordteacher.shared.features.definitions.di.DefinitionsModule
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.arkivanov.decompose.ComponentContext
import com.russhwolf.settings.coroutines.FlowSettings
import dagger.BindsInstance
import dagger.Component

@Component(
    dependencies = [ArticleDependencies::class, DefinitionsDependencies::class],
    modules = [ArticleModule::class, DefinitionsModule::class]
)
interface ArticleComposeComponent {
    fun articleDecomposeComponent(): ArticleDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance
        fun setInitialState(state: ArticleVM.State): Builder

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