package com.aglushkov.wordteacher.androidApp.features.add_article.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.add_article.views.AddArticleFragment
import com.aglushkov.wordteacher.androidApp.features.add_article.views.AddArticleVMWrapper
import com.aglushkov.wordteacher.di.AddArticleModule
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleVM
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import dagger.BindsInstance
import dagger.Component

@FragmentComp
@Component(dependencies = [AddArticleDependencies::class], modules = [AddArticleModule::class])
interface AddArticleComponent {
    fun injectAddArticleFragment(fragment: AddArticleFragment)
    fun injectViewModelWrapper(fragment: AddArticleVMWrapper)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun setVMWrapper(vmWrapper: AddArticleVMWrapper): Builder

        @BindsInstance
        fun setVMState(state: AddArticleVM.State): Builder

        fun setDeps(deps: AddArticleDependencies): Builder
        fun build(): AddArticleComponent
    }
}

interface AddArticleDependencies {
    fun articleRepository(): ArticleRepository
}