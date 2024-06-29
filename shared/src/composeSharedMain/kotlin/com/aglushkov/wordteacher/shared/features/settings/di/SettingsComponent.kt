package com.aglushkov.wordteacher.shared.features.settings.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.di.IsDebug
import com.aglushkov.wordteacher.shared.di.WordFrequencyFileOpener
import com.aglushkov.wordteacher.shared.features.settings.SettingsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.settings.vm.FileSharer
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsVM
import com.aglushkov.wordteacher.shared.general.FileOpenController
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.repository.logs.LogsRepository
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component
import javax.inject.Qualifier

@Component(dependencies = [SettingsDependencies::class], modules = [SettingsModule::class])
interface SettingsComponent {
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
    fun spaceAuthRepository(): SpaceAuthRepository
    fun connectivityManager(): ConnectivityManager
    fun idGenerator(): IdGenerator
    fun logsRepository(): LogsRepository
    fun fileSharer(): FileSharer?
    fun wordFrequencyGradationProvider(): WordFrequencyGradationProvider
    @WordFrequencyFileOpener fun wordFrequencyFileOpenController(): FileOpenController
    fun analytics(): Analytics
}
