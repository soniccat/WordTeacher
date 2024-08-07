package com.aglushkov.wordteacher.shared.features.dict_configs.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.di.DslFileOpener
import com.aglushkov.wordteacher.shared.di.IsDebug
import com.aglushkov.wordteacher.shared.features.dict_configs.DictConfigsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.settings.SettingsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsVM
import com.aglushkov.wordteacher.shared.general.FileOpenController
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class DictConfigsModule {
    @Provides
    fun dictConfigsDecomposeComponent(
        componentContext: ComponentContext,
        configRepository: ConfigRepository,
        @DslFileOpener dslDictOpenController: FileOpenController,
        dictRepository: DictRepository,
        idGenerator: IdGenerator,
        analytics: Analytics,
    ) = DictConfigsDecomposeComponent(
        componentContext,
        configRepository,
        dslDictOpenController,
        dictRepository,
        idGenerator,
        analytics,
    )
}
