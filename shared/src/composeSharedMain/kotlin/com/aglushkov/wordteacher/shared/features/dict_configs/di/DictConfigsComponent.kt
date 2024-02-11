package com.aglushkov.wordteacher.shared.features.dict_configs.di

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
import dagger.BindsInstance
import dagger.Component
import javax.inject.Qualifier

@Component(dependencies = [DictConfigsDependencies::class], modules = [DictConfigsModule::class])
interface DictConfigsComponent {
    fun dictConfigsDecomposeComponent(): DictConfigsDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder

        fun setDeps(deps: DictConfigsDependencies): Builder
        fun build(): DictConfigsComponent
    }
}

interface DictConfigsDependencies {
    fun configRepository(): ConfigRepository
    fun idGenerator(): IdGenerator
    @DslFileOpener
    fun dslDictOpenController(): FileOpenController
    fun dictRepository(): DictRepository
}
