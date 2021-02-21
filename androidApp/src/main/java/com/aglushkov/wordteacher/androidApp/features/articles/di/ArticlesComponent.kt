package com.aglushkov.wordteacher.androidApp.features.articles.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.articles.views.ArticlesFragment
import com.aglushkov.wordteacher.androidApp.features.articles.views.ArticlesVMWrapper
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.di.ArticlesModule
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesRouter
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import dagger.BindsInstance
import dagger.Component

@FragmentComp
@Component(dependencies = [ArticlesDependencies::class], modules = [ArticlesModule::class])
interface ArticlesComponent {
    fun injectArticlesFragment(fragment: ArticlesFragment)
    fun injectViewModelWrapper(fragment: ArticlesVMWrapper)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun setVMWrapper(vmWrapper: ArticlesVMWrapper): Builder

        fun setDeps(deps: ArticlesDependencies): Builder
        fun build(): ArticlesComponent
    }
}

interface ArticlesDependencies {
    fun routerResolver(): RouterResolver
    fun articleRepository(): ArticleRepository
    fun idGenerator(): IdGenerator
}