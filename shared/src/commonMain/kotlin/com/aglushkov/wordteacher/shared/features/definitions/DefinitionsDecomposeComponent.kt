package com.aglushkov.wordteacher.shared.features.definitions

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent.Companion
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsRouter
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVMImpl
import com.aglushkov.wordteacher.shared.general.AudioService
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.clipboard.ClipboardRepository
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionHistoryRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.statekeeper.consume


class DefinitionsDecomposeComponent (
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
    settings: SettingStore,
    wordDefinitionHistoryRepository: WordDefinitionHistoryRepository,
    audioService: AudioService,
) : DefinitionsVMImpl(
    componentContext.stateKeeper.consume(
        key = KEY_STATE,
        strategy = DefinitionsVM.State.serializer()
    ) ?: initialState,
    connectivityManager,
    wordDefinitionRepository,
    dictRepository,
    cardSetsRepository,
    wordFrequencyGradationProvider,
    wordTeacherDictService,
    definitionsSettings,
    clipboardRepository,
    idGenerator,
    analytics,
    settings,
    wordDefinitionHistoryRepository,
    audioService,
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_Definitions"

    init {
        baseInit(analytics)

        stateKeeper.register(
            key = KEY_STATE,
            strategy = DefinitionsVM.State.serializer()
        ) { state }
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}
