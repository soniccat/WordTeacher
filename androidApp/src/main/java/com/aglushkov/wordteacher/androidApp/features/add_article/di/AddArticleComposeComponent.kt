package com.aglushkov.wordteacher.androidApp.features.add_article.di

import com.aglushkov.wordteacher.di.AddArticleComposeModule
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [AddArticleDependencies::class], modules = [AddArticleComposeModule::class])
interface AddArticleComposeComponent {
    fun buildAddArticleDecomposeComponent(): AddArticleDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setConfiguration(configuration: AddArticleVM.State): Builder

        fun setDeps(deps: AddArticleDependencies): Builder
        fun build(): AddArticleComposeComponent
    }
}