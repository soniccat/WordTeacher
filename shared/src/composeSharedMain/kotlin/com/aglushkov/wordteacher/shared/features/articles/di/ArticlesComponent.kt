package com.aglushkov.wordteacher.shared.features.articles.di

import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.articles.ArticlesDecomposeComponent
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [ArticlesDependencies::class], modules = [ArticlesModule::class])
interface ArticlesComposeComponent {
    fun articlesDecomposeComponent(): ArticlesDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setConfiguration(configuration: TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration): Builder

        fun setDeps(deps: ArticlesDependencies): Builder
        fun build(): ArticlesComposeComponent
    }
}

interface ArticlesDependencies {
    fun articlesRepository(): ArticlesRepository
    fun idGenerator(): IdGenerator
    fun timeSource(): TimeSource
}