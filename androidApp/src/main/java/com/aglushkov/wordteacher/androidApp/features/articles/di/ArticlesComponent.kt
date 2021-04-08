package com.aglushkov.wordteacher.androidApp.features.articles.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.articles.views.ArticlesFragment
import com.aglushkov.wordteacher.androidApp.features.articles.views.ArticlesAndroidVM
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.di.ArticlesModule
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import dagger.BindsInstance
import dagger.Component

@FragmentComp
@Component(dependencies = [ArticlesDependencies::class], modules = [ArticlesModule::class])
interface ArticlesComponent {
    fun injectArticlesFragment(fragment: ArticlesFragment)
    fun injectViewModelWrapper(fragment: ArticlesAndroidVM)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun setVMWrapper(vmWrapper: ArticlesAndroidVM): Builder

        fun setDeps(deps: ArticlesDependencies): Builder
        fun build(): ArticlesComponent
    }
}

interface ArticlesDependencies {
    fun routerResolver(): RouterResolver
    fun articlesRepository(): ArticlesRepository
    fun idGenerator(): IdGenerator
}