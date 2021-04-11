package com.aglushkov.wordteacher.androidApp.features.add_article.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.add_article.views.AddArticleFragment
import com.aglushkov.wordteacher.androidApp.features.add_article.views.AddArticleAndroidVM
import com.aglushkov.wordteacher.di.AddArticleModule
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import dagger.BindsInstance
import dagger.Component

@FragmentComp
@Component(dependencies = [AddArticleDependencies::class], modules = [AddArticleModule::class])
interface AddArticleComponent {
    fun injectAddArticleFragment(fragment: AddArticleFragment)
    fun injectViewModelWrapper(fragment: AddArticleAndroidVM)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun setVMWrapper(vmWrapper: AddArticleAndroidVM): Builder

        @BindsInstance
        fun setVMState(state: AddArticleVM.State): Builder

        fun setDeps(deps: AddArticleDependencies): Builder
        fun build(): AddArticleComponent
    }
}

interface AddArticleDependencies {
    fun articlesRepository(): ArticlesRepository
    fun timeSource(): TimeSource
}