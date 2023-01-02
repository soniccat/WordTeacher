package com.aglushkov.wordteacher.androidApp.features.settings.di

import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.settings.SettingsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [SettingsDependencies::class], modules = [SettingsModule::class])
public interface SettingsComponent {
    fun settingsDecomposeComponent(): SettingsDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setState(state: SettingsVM.State): Builder

        fun setDeps(deps: SettingsDependencies): Builder
        fun build(): SettingsComponent
    }
}

interface SettingsDependencies {
    fun routerResolver(): RouterResolver
    fun spaceAuthRepository(): SpaceAuthRepository
    fun connectivityManager(): ConnectivityManager
    fun idGenerator(): IdGenerator
}
