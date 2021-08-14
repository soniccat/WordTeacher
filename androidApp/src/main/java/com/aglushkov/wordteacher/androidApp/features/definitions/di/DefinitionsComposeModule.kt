package com.aglushkov.wordteacher.androidApp.features.definitions.di

import com.aglushkov.wordteacher.shared.features.RootDecomposeComponent
import com.aglushkov.wordteacher.shared.features.RootDecomposeComponentImpl
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class DefinitionsComposeModule {
    @Provides
    fun definitionsDecomposeComponentFactory(
        connectivityManager: ConnectivityManager,
        wordDefinitionRepository: WordDefinitionRepository,
        idGenerator: IdGenerator,
    ): (context: ComponentContext, configuration: RootDecomposeComponent.ChildConfiguration) -> DefinitionsDecomposeComponent =
        { context: ComponentContext, configuration: RootDecomposeComponent.ChildConfiguration ->
            when (configuration) {
                is RootDecomposeComponent.ChildConfiguration.DefinitionConfiguration -> DefinitionsDecomposeComponent(
                    context,
                    configuration.word,
                    connectivityManager,
                    wordDefinitionRepository,
                    idGenerator
                )
                is RootDecomposeComponent.ChildConfiguration.ArticlesConfiguration -> DefinitionsDecomposeComponent(
                    context,
                    "fox",
                    connectivityManager,
                    wordDefinitionRepository,
                    idGenerator
                )
            }

        }

    @JvmSuppressWildcards
    @Provides
    fun rootComponent(
        componentContext: ComponentContext,
        definitionsDecomposeComponentFactory: (context: ComponentContext, configuration: RootDecomposeComponent.ChildConfiguration) -> DefinitionsDecomposeComponent
    ) : RootDecomposeComponent {
        return RootDecomposeComponentImpl(
            componentContext,
            childComponentFactory = definitionsDecomposeComponentFactory
        )
    }
}

