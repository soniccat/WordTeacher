package com.aglushkov.wordteacher.shared.features.definitions.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.clipboard.ClipboardRepository
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionHistoryRepository
import com.arkivanov.decompose.ComponentContext
import com.russhwolf.settings.coroutines.FlowSettings
import dagger.Module
import dagger.Provides

@Module
class DefinitionsModule {
    @Provides
    fun definitionsDecomposeComponent(
        componentContext: ComponentContext,
        initialState: DefinitionsVM.State,
        definitionsSettings: DefinitionsVM.Settings,
        connectivityManager: ConnectivityManager,
        wordDefinitionRepository: WordDefinitionRepository,
        dictRepository: DictRepository,
        cardSetsRepository: CardSetsRepository,
        wordFrequencyGradationProvider: WordFrequencyGradationProvider,
        wordTeacherDictService: WordTeacherDictService,
        clipboardRepository: ClipboardRepository,
        idGenerator: IdGenerator,
        analytics: Analytics,
        settings: FlowSettings,
        wordDefinitionHistoryRepository: WordDefinitionHistoryRepository,
    ) = DefinitionsDecomposeComponent(
        componentContext,
        initialState,
        definitionsSettings,
        connectivityManager,
        wordDefinitionRepository,
        dictRepository,
        cardSetsRepository,
        wordFrequencyGradationProvider,
        wordTeacherDictService,
        clipboardRepository,
        idGenerator,
        analytics,
        settings,
        wordDefinitionHistoryRepository,
    )
}
