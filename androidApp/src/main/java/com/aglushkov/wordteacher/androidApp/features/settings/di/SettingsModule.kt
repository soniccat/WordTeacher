package com.aglushkov.wordteacher.androidApp.features.settings.di

import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.settings.SettingsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsVM
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class SettingsModule {
    @Provides
    fun settingsDecomposeComponent(
        componentContext: ComponentContext,
        state: SettingsVM.State,
        connectivityManager: ConnectivityManager,
        spaceAuthRepository: SpaceAuthRepository,
        idGenerator: IdGenerator,
    ) = SettingsDecomposeComponent(
        componentContext,
        state,
        connectivityManager,
        spaceAuthRepository,
        idGenerator
    )
}