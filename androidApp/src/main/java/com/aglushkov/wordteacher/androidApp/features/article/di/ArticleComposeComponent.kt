package com.aglushkov.wordteacher.androidApp.features.article.di

import android.content.Context
import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DefinitionsComposeComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DefinitionsComposeModule
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DefinitionsDependencies
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DefinitionsModule
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsAndroidVM
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.ArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component
import javax.inject.Qualifier

@FragmentComp
@Component(
    dependencies = [ArticleDependencies::class, DefinitionsDependencies::class],
    modules = [ArticleComposeModule::class, DefinitionsComposeModule::class]
)
interface ArticleComposeComponent {
    fun articleDecomposeComponent(): ArticleDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setConfiguration(configuration: MainDecomposeComponent.ChildConfiguration.ArticleConfiguration): Builder

//        @BindsInstance fun setVMWrapper(vmWrapper: ArticleAndroidVM): Builder
//        @BindsInstance fun setVMState(state: ArticleVM.State): Builder
//        @BindsInstance fun setDefinitionsVMWrapper(vmWrapper: DefinitionsAndroidVM): Builder
//        @BindsInstance fun setDefinitionsState(state: DefinitionsVM.State): Builder
//        @BindsInstance fun setViewContext(@ViewContext context: Context): Builder

        fun setDeps(deps: ArticleDependencies): Builder
        fun setDefinitionsDeps(deps: DefinitionsDependencies): Builder
        fun build(): ArticleComposeComponent
    }
}

interface ArticleDependencies {
    fun database(): AppDatabase
    fun nlpCore(): NLPCore
}