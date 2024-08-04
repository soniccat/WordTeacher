package com.aglushkov.wordteacher.shared.features.webauth.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.di.IsDebug
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.features.webauth.WebAuthDecomposeComponent
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.oauth2.OAuth2Service
import com.aglushkov.wordteacher.shared.repository.article.ArticleParserRepository
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class WebAuthModule {

    @Provides
    fun viewModel(
        componentContext: ComponentContext,
        configuration: MainDecomposeComponent.ChildConfiguration.WebAuthConfiguration,
        timeSource: TimeSource,
        googleOAuth2Service: OAuth2Service,
        analytics: Analytics,
        @IsDebug isDebug: Boolean,
    ): WebAuthDecomposeComponent {
        return WebAuthDecomposeComponent(
            componentContext,
            configuration,
            timeSource,
            googleOAuth2Service,
            analytics,
            isDebug
        )
    }
}