package com.aglushkov.wordteacher.android_app.di

import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
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
    fun articleParserRepository(): ArticleParserRepository = ArticleParserRepository()

    @Provides
    fun viewModel(
        componentContext: ComponentContext,
        state: AddArticleVM.State,
        articlesRepository: ArticlesRepository,
        articleParserRepository: ArticleParserRepository,
        cardSetsRepository: CardSetsRepository,
        timeSource: TimeSource
    ): AddArticleDecomposeComponent {
        return AddArticleDecomposeComponent(
            componentContext,
            articlesRepository,
            articleParserRepository,
            cardSetsRepository,
            timeSource,
            state
        )
    }
}