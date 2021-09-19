package com.aglushkov.wordteacher.androidApp.features.articles.di

import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.articles.ArticlesDecomposeComponent
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [ArticlesDependencies::class], modules = [ArticlesComposeModule::class])
public interface ArticlesComposeComponent {
    fun articlesDecomposeComponent(): ArticlesDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setConfiguration(configuration: TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration): Builder

        fun setDeps(deps: ArticlesDependencies): Builder
        fun build(): ArticlesComposeComponent
    }
}
