package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleRouter
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import dagger.Module
import dagger.Provides

@Module
class ArticleModule {

    @FragmentComp
    @Provides
    fun viewModel(
        definitionsVM: DefinitionsVM,
        routerResolver: RouterResolver,
        articlesRepository: ArticleRepository,
        state: ArticleVM.State
    ): ArticleVM {
        return ArticleVM(definitionsVM, articlesRepository, state, object : ArticleRouter {
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