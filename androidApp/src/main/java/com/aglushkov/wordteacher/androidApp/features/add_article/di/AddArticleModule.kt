package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.articles.blueprints.ArticleBlueprint
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleVM
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import dagger.Module
import dagger.Provides

@Module
class AddArticleModule {

    @FragmentComp
    @Provides
    fun viewModel(
        articleRepository: ArticleRepository,
        state: AddArticleVM.State
    ): AddArticleVM {
        return AddArticleVM(articleRepository, state)
    }
}