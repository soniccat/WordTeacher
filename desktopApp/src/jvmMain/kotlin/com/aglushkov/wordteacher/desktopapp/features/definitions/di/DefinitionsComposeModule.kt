package com.aglushkov.wordteacher.desktopapp.features.definitions.di

import com.aglushkov.wordteacher.shared.features.ChildConfiguration
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponentImpl
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Provides

@Module
class DefinitionsComposeModule {
    @Provides
    fun definitionsDecomposeComponentFactory(
        connectivityManager: ConnectivityManager,
        wordDefinitionRepository: WordDefinitionRepository,
        idGenerator: IdGenerator,
    ): (context: ComponentContext, configuration: ChildConfiguration) -> DefinitionsDecomposeComponent =
        { context: ComponentContext, configuration: ChildConfiguration ->
            DefinitionsDecomposeComponent(
                context,
                configuration.word,
                connectivityManager,
                wordDefinitionRepository,
                idGenerator
            )
        }

    @JvmSuppressWildcards
    @Provides
    fun rootComponent(
        componentContext: ComponentContext,
        definitionsDecomposeComponentFactory: (context: ComponentContext, configuration: ChildConfiguration) -> DefinitionsDecomposeComponent
    ) : TabDecomposeComponent {
        return TabDecomposeComponentImpl(
            componentContext,
            childComponentFactory = definitionsDecomposeComponentFactory
        )
    }
}

