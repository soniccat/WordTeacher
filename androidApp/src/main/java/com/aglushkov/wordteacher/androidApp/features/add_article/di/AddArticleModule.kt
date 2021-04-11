package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import dagger.Module
import dagger.Provides

@Module
class AddArticleModule {

    @FragmentComp
    @Provides
    fun viewModel(
        articlesRepository: ArticlesRepository,
        timeSource: TimeSource,
        state: AddArticleVM.State
    ): AddArticleVM {
        return AddArticleVM(articlesRepository, timeSource, state)
    }
}