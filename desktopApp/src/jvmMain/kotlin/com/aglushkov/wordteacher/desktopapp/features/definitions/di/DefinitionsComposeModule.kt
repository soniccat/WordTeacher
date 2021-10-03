package com.aglushkov.wordteacher.desktopapp.features.definitions.di

import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponentImpl
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Provides
import dagger.Module

@Module
class DefinitionsComposeModule {
    @Provides
    fun definitionsDecomposeComponentFactory(
        connectivityManager: ConnectivityManager,
        wordDefinitionRepository: WordDefinitionRepository,
        idGenerator: IdGenerator,
    ): (context: ComponentContext, configuration: TabDecomposeComponent.ChildConfiguration) -> DefinitionsDecomposeComponent =
        { context: ComponentContext, configuration: TabDecomposeComponent.ChildConfiguration ->
            when (configuration) {
                is TabDecomposeComponent.ChildConfiguration.DefinitionConfiguration ->
                    DefinitionsDecomposeComponent(
                        context,
                        configuration.word,
                        connectivityManager,
                        wordDefinitionRepository,
                        idGenerator
                    )
                else -> throw RuntimeException("Unsupported configuration")
            }
        }

    @JvmSuppressWildcards
    @Provides
    fun rootComponent(
        componentContext: ComponentContext,
        definitionsDecomposeComponentFactory: (context: ComponentContext, configuration: TabDecomposeComponent.ChildConfiguration) -> DefinitionsDecomposeComponent
    ) : TabDecomposeComponent {
        return TabDecomposeComponentImpl(
            componentContext,
            childComponentFactory = definitionsDecomposeComponentFactory
        )
    }
}

