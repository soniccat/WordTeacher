package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.articles.blueprints.ArticleBlueprint
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesRouter
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVMImpl
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
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
        routerResolver: RouterResolver,
        articlesRepository: ArticlesRepository,
        time: TimeSource
    ): ArticlesVM {
        return ArticlesVMImpl(articlesRepository, time, object : ArticlesRouter {
            override fun openAddArticle() {
                routerResolver.router?.get()?.openAddArticle()
            }

            override fun openArticle(id: Long) {
                routerResolver.router?.get()?.openArticle(id)
            }
        })
    }
}