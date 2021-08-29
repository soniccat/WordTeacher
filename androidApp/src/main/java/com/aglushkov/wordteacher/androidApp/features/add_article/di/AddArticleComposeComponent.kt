package com.aglushkov.wordteacher.androidApp.features.add_article.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.add_article.views.AddArticleFragment
import com.aglushkov.wordteacher.androidApp.features.add_article.views.AddArticleAndroidVM
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DefinitionsComposeComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DefinitionsDependencies
import com.aglushkov.wordteacher.di.AddArticleComposeModule
import com.aglushkov.wordteacher.di.AddArticleModule
import com.aglushkov.wordteacher.shared.features.RootDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [AddArticleDependencies::class], modules = [AddArticleComposeModule::class])
interface AddArticleComposeComponent {
    fun buildAddArticleDecomposeComponent(): AddArticleDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setConfiguration(configuration: RootDecomposeComponent.ChildConfiguration.AddArticleConfiguration): Builder

        fun setDeps(deps: AddArticleDependencies): Builder
        fun build(): AddArticleComposeComponent
    }
}
