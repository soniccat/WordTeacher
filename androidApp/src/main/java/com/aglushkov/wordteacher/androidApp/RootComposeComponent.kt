package com.aglushkov.wordteacher.androidApp.features.definitions.di

import com.aglushkov.wordteacher.androidApp.RootComposeModule
import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.RootDecomposeComponent
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesRouter
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@FragmentComp
@Component(dependencies =
    [
        CommonComposeDependencies::class,
        DefinitionsDependencies::class,
        ArticlesComposeDependencies::class
    ],
    modules = [RootComposeModule::class])
public interface RootComposeComponent {
    fun rootDecomposeComponent(): RootDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setWord(word: String?): Builder
        @BindsInstance fun setArticlesRouter(router: ArticlesRouter): Builder

        fun setCommonDeps(deps: CommonComposeDependencies): Builder
        fun setDefinitionsDeps(deps: DefinitionsDependencies): Builder
        fun setArticlesDeps(deps: ArticlesComposeDependencies): Builder
        fun build(): RootComposeComponent
    }
}

interface CommonComposeDependencies {
    fun idGenerator(): IdGenerator
}

interface ArticlesComposeDependencies {
    fun routerResolver(): RouterResolver
    fun articlesRepository(): ArticlesRepository
    fun timeSource(): TimeSource
}