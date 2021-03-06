package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleRouter
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import dagger.Module
import dagger.Provides

@Module
class ArticleModule {

    @FragmentComp
    @Provides
    fun viewModel(
        routerResolver: RouterResolver,
        articlesRepository: ArticleRepository,
        state: ArticleVM.State
    ): ArticleVM {
        return ArticleVM(articlesRepository, state, object : ArticleRouter {
            override fun closeArticle() {
                routerResolver.router?.get()?.closeArticle()
            }
        })
    }

    @FragmentComp
    @Provides
    fun articleRepository(
        database: AppDatabase,
        nlpCore: NLPCore
    ) = ArticleRepository(database, nlpCore)
}