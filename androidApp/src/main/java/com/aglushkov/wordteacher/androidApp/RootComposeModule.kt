package com.aglushkov.wordteacher.androidApp

import com.aglushkov.wordteacher.androidApp.features.add_article.di.DaggerAddArticleComposeComponent
import com.aglushkov.wordteacher.androidApp.features.articles.di.DaggerArticlesComposeComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DaggerDefinitionsComposeComponent
import com.aglushkov.wordteacher.di.AppComponent
import com.aglushkov.wordteacher.shared.features.RootDecomposeComponent
import com.aglushkov.wordteacher.shared.features.RootDecomposeComponentImpl
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class RootComposeModule {
    @Provides
    fun rootDecomposeComponentFactory(
        appComponent: AppComponent
    ): (context: ComponentContext, configuration: RootDecomposeComponent.ChildConfiguration) -> Any =
        { context: ComponentContext, configuration: RootDecomposeComponent.ChildConfiguration ->
            when (configuration) {
                is RootDecomposeComponent.ChildConfiguration.DefinitionConfiguration ->
                    DaggerDefinitionsComposeComponent.builder()
                        .setComponentContext(context)
                        .setConfiguration(configuration)
                        .setDeps(appComponent)
                        .build()
                        .definitionsDecomposeComponent()
                is RootDecomposeComponent.ChildConfiguration.ArticlesConfiguration ->
                    DaggerArticlesComposeComponent.builder()
                        .setComponentContext(context)
                        .setConfiguration(configuration)
                        .setDeps(appComponent)
                        .build()
                        .articlesDecomposeComponent()
                is RootDecomposeComponent.ChildConfiguration.AddArticleConfiguration ->
                    DaggerAddArticleComposeComponent.builder()
                        .setComponentContext(context)
                        .setConfiguration(configuration)
                        .setDeps(appComponent)
                        .build()
                        .buildAddArticleDecomposeComponent()
                is RootDecomposeComponent.ChildConfiguration.EmptyDialogConfiguration ->
                    Any()
            }

        }

    @JvmSuppressWildcards
    @Provides
    fun rootComponent(
        componentContext: ComponentContext,
        rootDecomposeComponentFactory: (context: ComponentContext, configuration: RootDecomposeComponent.ChildConfiguration) -> Any
    ) : RootDecomposeComponent {
        return RootDecomposeComponentImpl(
            componentContext,
            childComponentFactory = rootDecomposeComponentFactory
        )
    }
}

