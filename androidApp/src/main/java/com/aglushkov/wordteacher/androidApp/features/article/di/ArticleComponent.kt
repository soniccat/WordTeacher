package com.aglushkov.wordteacher.androidApp.features.article.di

import android.content.Context
import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.article.views.ArticleFragment
import com.aglushkov.wordteacher.androidApp.features.article.views.ArticleAndroidVM
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DefinitionsDependencies
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DefinitionsModule
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsAndroidVM
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import dagger.BindsInstance
import dagger.Component
import javax.inject.Qualifier

@FragmentComp
@Component(
    dependencies = [ArticleDependencies::class, DefinitionsDependencies::class],
    modules = [ArticleModule::class, DefinitionsModule::class]
)
interface ArticleComponent {
    fun injectArticleFragment(fragment: ArticleFragment)
    fun injectViewModelWrapper(fragment: ArticleAndroidVM)
    fun injectDefinitionsViewModelWrapper(fragment: DefinitionsAndroidVM)

    @Component.Builder
    interface Builder {
        @BindsInstance fun setVMWrapper(vmWrapper: ArticleAndroidVM): Builder
        @BindsInstance fun setVMState(state: ArticleVM.State): Builder
        @BindsInstance fun setDefinitionsVMWrapper(vmWrapper: DefinitionsAndroidVM): Builder
        @BindsInstance fun setDefinitionsState(state: DefinitionsVM.State): Builder
        @BindsInstance fun setViewContext(@ViewContext context: Context): Builder

        fun setDeps(deps: ArticleDependencies): Builder
        fun setDefinitionsDeps(deps: DefinitionsDependencies): Builder
        fun build(): ArticleComponent
    }
}

interface ArticleDependencies {
    fun database(): AppDatabase
    fun nlpCore(): NLPCore
}

//@kotlin.annotation.Target(AnnotationTarget.TYPE)
@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class ViewContext