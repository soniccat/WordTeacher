package com.aglushkov.wordteacher.shared.features.settings

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent.Companion
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsRouter
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVMImpl
import com.aglushkov.wordteacher.shared.features.settings.vm.FileSharer
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsVM
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsVMImpl
import com.aglushkov.wordteacher.shared.general.AppInfo
import com.aglushkov.wordteacher.shared.general.EmailOpener
import com.aglushkov.wordteacher.shared.general.FileOpenController
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.WebLinkOpener
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.logs.LogsRepository
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.statekeeper.consume
import com.russhwolf.settings.coroutines.FlowSettings

class SettingsDecomposeComponent (
    componentContext: ComponentContext,
    initialState: SettingsVM.State,
    connectivityManager: ConnectivityManager,
    spaceAuthRepository: SpaceAuthRepository,
    logsRepository: LogsRepository,
    idGenerator: IdGenerator,
    isDebug: Boolean,
    fileSharer: FileSharer?,
    wordFrequencyGradationProvider: WordFrequencyGradationProvider,
    wordFrequencyFileOpenController: FileOpenController,
    analytics: Analytics,
    appInfo: AppInfo,
    emailOpener: EmailOpener,
    webLinkOpener: WebLinkOpener,
    databaseCardWorker: DatabaseCardWorker,
    settings: FlowSettings
) : SettingsVMImpl(
    componentContext.stateKeeper.consume(
        key = KEY_STATE,
        strategy = SettingsVM.State.serializer()
    ) ?: initialState,
    connectivityManager,
    spaceAuthRepository,
    logsRepository,
    idGenerator,
    isDebug,
    fileSharer,
    wordFrequencyGradationProvider,
    wordFrequencyFileOpenController,
    analytics,
    appInfo,
    emailOpener,
    webLinkOpener,
    databaseCardWorker,
    settings,
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_Settings"

    init {
        baseInit(analytics)

        stateKeeper.register(
            key = KEY_STATE,
            strategy = SettingsVM.State.serializer()
        ) { this.state }
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}
