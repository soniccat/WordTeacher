package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.general.Time
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import dagger.Module
import dagger.Provides

@Module
class AddArticleModule {

    @FragmentComp
    @Provides
    fun viewModel(
        articlesRepository: ArticlesRepository,
        time: Time,
        state: AddArticleVM.State
    ): AddArticleVM {
        return AddArticleVM(articlesRepository, time, state)
    }
}