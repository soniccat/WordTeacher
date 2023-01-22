package com.aglushkov.wordteacher.android_app.features.settings.di

import com.aglushkov.wordteacher.android_app.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.settings.SettingsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component
import javax.inject.Qualifier

@Component(dependencies = [SettingsDependencies::class], modules = [SettingsModule::class])
public interface SettingsComponent {
    fun settingsDecomposeComponent(): SettingsDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setState(state: SettingsVM.State): Builder
        @BindsInstance fun setIsDebug(@IsDebug isDebug: Boolean): Builder

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

@Qualifier
annotation class IsDebug
