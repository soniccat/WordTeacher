package com.aglushkov.wordteacher.shared.features.articles.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.articles.ArticlesDecomposeComponent
import com.aglushkov.wordteacher.shared.general.IdGenerator
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
        idGenerator: IdGenerator,
        timeSource: TimeSource,
        analytics: Analytics,
    ) = ArticlesDecomposeComponent(
        componentContext,
        articlesRepository,
        idGenerator,
        timeSource,
        analytics,
    )
}
