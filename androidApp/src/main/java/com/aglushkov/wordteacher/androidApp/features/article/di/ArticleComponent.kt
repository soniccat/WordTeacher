package com.aglushkov.wordteacher.androidApp.features.article.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.article.views.ArticleFragment
import com.aglushkov.wordteacher.androidApp.features.article.views.ArticleVMWrapper
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsVMWrapper
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.di.ArticleModule
import com.aglushkov.wordteacher.di.DefinitionsComponent
import com.aglushkov.wordteacher.di.DefinitionsDependencies
import com.aglushkov.wordteacher.di.DefinitionsModule
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import dagger.BindsInstance
import dagger.Component

@FragmentComp
@Component(
    dependencies = [ArticleDependencies::class, DefinitionsDependencies::class],
    modules = [ArticleModule::class, DefinitionsModule::class]
)
interface ArticleComponent {
    fun injectArticleFragment(fragment: ArticleFragment)
    fun injectViewModelWrapper(fragment: ArticleVMWrapper)
    fun injectDefinitionsViewModelWrapper(fragment: DefinitionsVMWrapper)

    @Component.Builder
    interface Builder {
        @BindsInstance fun setVMWrapper(vmWrapper: ArticleVMWrapper): Builder
        @BindsInstance fun setDefinitionsState(state: DefinitionsVM.State): Builder
        @BindsInstance fun setVMState(state: ArticleVM.State): Builder

        fun setDeps(deps: ArticleDependencies): Builder
        fun setDefinitionsDeps(deps: DefinitionsDependencies): Builder
        fun build(): ArticleComponent
    }
}

interface ArticleDependencies {
    fun routerResolver(): RouterResolver
    fun database(): AppDatabase
    fun nlpCore(): NLPCore
}