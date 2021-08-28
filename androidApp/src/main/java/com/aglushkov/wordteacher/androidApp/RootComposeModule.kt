package com.aglushkov.wordteacher.androidApp

import com.aglushkov.wordteacher.androidApp.features.articles.di.ArticlesDependencies
import com.aglushkov.wordteacher.androidApp.features.articles.di.DaggerArticlesComposeComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DaggerDefinitionsComposeComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DefinitionsDependencies
import com.aglushkov.wordteacher.shared.features.RootDecomposeComponent
import com.aglushkov.wordteacher.shared.features.RootDecomposeComponentImpl
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class RootComposeModule {
    @Provides
    fun rootDecomposeComponentFactory(
        definitionsDeps: DefinitionsDependencies,
        articlesDeps: ArticlesDependencies
    ): (context: ComponentContext, configuration: RootDecomposeComponent.ChildConfiguration) -> Any =
        { context: ComponentContext, configuration: RootDecomposeComponent.ChildConfiguration ->
            when (configuration) {
                is RootDecomposeComponent.ChildConfiguration.DefinitionConfiguration ->
                    DaggerDefinitionsComposeComponent.builder()
                        .setComponentContext(context)
                        .setConfiguration(configuration)
                        .setDeps(definitionsDeps)
                        .build()
                        .definitionsDecomposeComponent()
                is RootDecomposeComponent.ChildConfiguration.ArticlesConfiguration ->
                    DaggerArticlesComposeComponent.builder()
                        .setComponentContext(context)
                        .setConfiguration(configuration)
                        .setDeps(articlesDeps)
                        .build()
                        .articlesDecomposeComponent()
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

