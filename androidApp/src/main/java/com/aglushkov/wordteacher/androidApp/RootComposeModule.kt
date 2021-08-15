package com.aglushkov.wordteacher.androidApp

import com.aglushkov.wordteacher.shared.features.RootDecomposeComponent
import com.aglushkov.wordteacher.shared.features.RootDecomposeComponentImpl
import com.aglushkov.wordteacher.shared.features.articles.ArticlesDecomposeComponent
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesRouter
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class RootComposeModule {
    @Provides
    fun rootDecomposeComponentFactory(
        // for DefinitionsDecomposeComponent
        connectivityManager: ConnectivityManager,
        wordDefinitionRepository: WordDefinitionRepository,
        idGenerator: IdGenerator,

        // for ArticlesDecomposeComponent
        articlesRepository: ArticlesRepository,
        timeSource: TimeSource,
        router: ArticlesRouter
    ): (context: ComponentContext, configuration: RootDecomposeComponent.ChildConfiguration) -> Any =
        { context: ComponentContext, configuration: RootDecomposeComponent.ChildConfiguration ->
            when (configuration) {
                is RootDecomposeComponent.ChildConfiguration.DefinitionConfiguration -> DefinitionsDecomposeComponent(
                    context,
                    configuration.word,
                    connectivityManager,
                    wordDefinitionRepository,
                    idGenerator
                )
                is RootDecomposeComponent.ChildConfiguration.ArticlesConfiguration -> ArticlesDecomposeComponent(
                    context,
                    articlesRepository,
                    timeSource,
                    router
                )
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

