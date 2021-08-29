package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.shared.features.RootDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class AddArticleComposeModule {

    @Provides
    fun viewModel(
        componentContext: ComponentContext,
        configuration: RootDecomposeComponent.ChildConfiguration.AddArticleConfiguration,
        articlesRepository: ArticlesRepository,
        timeSource: TimeSource
    ): AddArticleDecomposeComponent {
        return AddArticleDecomposeComponent(
            componentContext,
            articlesRepository,
            timeSource
        )
    }
}