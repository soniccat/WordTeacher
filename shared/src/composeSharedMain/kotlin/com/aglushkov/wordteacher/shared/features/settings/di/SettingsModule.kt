package com.aglushkov.wordteacher.shared.features.settings.di

import com.aglushkov.wordteacher.shared.di.IsDebug
import com.aglushkov.wordteacher.shared.features.settings.SettingsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.settings.vm.FileSharer
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.logs.LogsRepository
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
        logsRepository: LogsRepository,
        idGenerator: IdGenerator,
        @IsDebug isDebug: Boolean,
        fileSharer: FileSharer?,
    ) = SettingsDecomposeComponent(
        componentContext,
        state,
        connectivityManager,
        spaceAuthRepository,
        logsRepository,
        idGenerator,
        isDebug,
        fileSharer
    )
}
