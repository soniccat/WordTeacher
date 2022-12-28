package com.aglushkov.wordteacher.androidApp.features.articles.di

import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.articles.ArticlesDecomposeComponent
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class ArticlesModule {
    @Provides
    fun articleDecomposeComponent(
        componentContext: ComponentContext,
        configuration: TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration,
        articlesRepository: ArticlesRepository,
        timeSource: TimeSource,
        router: RouterResolver
    ) = ArticlesDecomposeComponent(
        componentContext,
        articlesRepository,
        timeSource,
        router.router!!.get()!!
    )
}
