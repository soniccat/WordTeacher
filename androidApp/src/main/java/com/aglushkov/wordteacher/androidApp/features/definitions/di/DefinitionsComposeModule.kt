package com.aglushkov.wordteacher.androidApp.features.definitions.di

import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsRouter
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
        router: RouterResolver,
        connectivityManager: ConnectivityManager,
        wordDefinitionRepository: WordDefinitionRepository,
        cardSetsRepository: CardSetsRepository,
        idGenerator: IdGenerator,
    ) = DefinitionsDecomposeComponent(
        componentContext,
        configuration.word,
        router.router!!.get()!!,
        connectivityManager,
        wordDefinitionRepository,
        cardSetsRepository,
        idGenerator
    )
}
