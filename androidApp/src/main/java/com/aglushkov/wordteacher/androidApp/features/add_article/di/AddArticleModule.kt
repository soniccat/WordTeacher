package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleVM
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import dagger.Module
import dagger.Provides

@Module
class AddArticleModule {

    @FragmentComp
    @Provides
    fun viewModel(
        articlesRepository: ArticlesRepository,
        state: AddArticleVM.State
    ): AddArticleVM {
        return AddArticleVM(articlesRepository, state)
    }
}