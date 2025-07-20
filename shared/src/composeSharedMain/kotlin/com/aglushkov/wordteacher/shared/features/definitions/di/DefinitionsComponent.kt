package com.aglushkov.wordteacher.shared.features.definitions.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.AudioService
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.clipboard.ClipboardRepository
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionHistoryRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.arkivanov.decompose.ComponentContext
import com.russhwolf.settings.coroutines.FlowSettings
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [DefinitionsDependencies::class], modules = [DefinitionsModule::class])
interface DefinitionsComposeComponent {
    fun definitionsDecomposeComponent(): DefinitionsDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setInitialState(state: DefinitionsVM.State): Builder
        @BindsInstance fun settings(settings: DefinitionsVM.Settings): Builder

        fun setDeps(deps: DefinitionsDependencies): Builder
        fun build(): DefinitionsComposeComponent
    }
}

interface DefinitionsDependencies {
    fun wordRepository(): WordDefinitionRepository
    fun connectivityManager(): ConnectivityManager
    fun idGenerator(): IdGenerator
    fun cardSetsRepository(): CardSetsRepository
    fun dictRepository(): DictRepository
    fun wordFrequencyGradationProvider(): WordFrequencyGradationProvider
    fun analytics(): Analytics
    fun wordTeacherDictService(): WordTeacherDictService
    fun settings(): SettingStore
    fun clipboardRepository(): ClipboardRepository
    fun wordDefinitionHistoryRepository(): WordDefinitionHistoryRepository
    fun audioService(): AudioService
}