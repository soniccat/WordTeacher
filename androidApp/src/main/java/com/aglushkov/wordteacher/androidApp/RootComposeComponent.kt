package com.aglushkov.wordteacher.androidApp.features.definitions.di

import com.aglushkov.wordteacher.androidApp.RootComposeModule
import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.articles.di.ArticlesDependencies
import com.aglushkov.wordteacher.shared.features.RootDecomposeComponent
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@FragmentComp
@Component(dependencies =
    [
        DefinitionsDependencies::class,
        ArticlesDependencies::class
    ],
    modules = [RootComposeModule::class])
public interface RootComposeComponent {
    fun rootDecomposeComponent(): RootDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setWord(word: String?): Builder

        fun setDefinitionsDeps(deps: DefinitionsDependencies): Builder
        fun setArticlesDeps(deps: ArticlesDependencies): Builder
        fun build(): RootComposeComponent
    }
}
