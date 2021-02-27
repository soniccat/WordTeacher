package com.aglushkov.wordteacher.androidApp.features.article.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.article.views.ArticleFragment
import com.aglushkov.wordteacher.androidApp.features.article.views.ArticleVMWrapper
import com.aglushkov.wordteacher.di.ArticleModule
import com.aglushkov.wordteacher.shared.features.article.ArticleVM
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import dagger.BindsInstance
import dagger.Component

@FragmentComp
@Component(dependencies = [ArticleDependencies::class], modules = [ArticleModule::class])
interface ArticleComponent {
    fun injectArticleFragment(fragment: ArticleFragment)
    fun injectViewModelWrapper(fragment: ArticleVMWrapper)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun setVMWrapper(vmWrapper: ArticleVMWrapper): Builder

        @BindsInstance
        fun setVMState(state: ArticleVM.State): Builder

        fun setDeps(deps: ArticleDependencies): Builder
        fun build(): ArticleComponent
    }
}

interface ArticleDependencies {
    fun database(): AppDatabase
    fun nlpCore(): NLPCore
}