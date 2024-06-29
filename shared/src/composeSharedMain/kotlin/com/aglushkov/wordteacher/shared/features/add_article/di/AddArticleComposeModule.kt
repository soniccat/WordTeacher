package com.aglushkov.wordteacher.shared.features.add_article.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.features.add_article.vm.ArticleContentExtractor
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticleParserRepository
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class AddArticleComposeModule {

    @Provides
    fun viewModel(
        componentContext: ComponentContext,
        state: AddArticleVM.State,
        articlesRepository: ArticlesRepository,
        contentExtractors: Array<ArticleContentExtractor>,
        cardSetsRepository: CardSetsRepository,
        timeSource: TimeSource,
        analytics: Analytics,
    ): AddArticleDecomposeComponent {
        return AddArticleDecomposeComponent(
            componentContext,
            articlesRepository,
            contentExtractors,
            cardSetsRepository,
            timeSource,
            analytics,
            state,
        )
    }
}