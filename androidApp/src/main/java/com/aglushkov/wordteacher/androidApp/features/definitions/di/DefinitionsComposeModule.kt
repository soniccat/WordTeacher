package com.aglushkov.wordteacher.androidApp.features.definitions.di

import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class DefinitionsComposeModule {
    @Provides
    fun definitionsDecomposeComponent(
        componentContext: ComponentContext,
        configuration: DefinitionsComposeComponent.DefinitionConfiguration,
        connectivityManager: ConnectivityManager,
        wordDefinitionRepository: WordDefinitionRepository,
        cardSetsRepository: CardSetsRepository,
        idGenerator: IdGenerator,
    ) = DefinitionsDecomposeComponent(
        componentContext,
        configuration.word,
        connectivityManager,
        wordDefinitionRepository,
        cardSetsRepository,
        idGenerator
    )
}
