package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.articles.blueprints.ArticleBlueprint
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import dagger.Module
import dagger.Provides

@Module
class ArticlesModule {
    @FragmentComp
    @Provides
    fun createItemViewBinder(
        articleBlueprint: ArticleBlueprint
    ): ViewItemBinder {
        return ViewItemBinder()
            .addBlueprint(articleBlueprint)
    }

    @FragmentComp
    @Provides
    fun viewModel(
        articleRepository: ArticleRepository,
        idGenerator: IdGenerator
    ): ArticlesVM {
        return ArticlesVM(articleRepository, idGenerator)
    }
}