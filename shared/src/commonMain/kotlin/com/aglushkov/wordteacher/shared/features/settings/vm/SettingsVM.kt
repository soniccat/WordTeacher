package com.aglushkov.wordteacher.shared.features.settings.vm

import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.general.AppInfo
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.EmailOpener
import com.aglushkov.wordteacher.shared.general.FileOpenController
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.WebLinkOpener
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.general.extensions.collectUntilDone
import com.aglushkov.wordteacher.shared.general.getAppInfo
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import com.aglushkov.wordteacher.shared.general.resource.downgradeToErrorOrLoading
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resource.onError
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.general.settings.resetHint
import com.aglushkov.wordteacher.shared.general.toStringResource
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradation
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.repository.logs.LogsRepository
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.repository.suggestion.SymSpellDictionaryHolder
import com.aglushkov.wordteacher.shared.repository.suggestion.SymSpellRepository
import com.aglushkov.wordteacher.shared.service.SpaceAuthData
import wordteacher.shared.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.*
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.workers.FillMisspellingDBController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import kotlinx.serialization.Serializable
import wordteacher.shared.generated.resources.error_default
import wordteacher.shared.generated.resources.settings_auth_refresh
import wordteacher.shared.generated.resources.settings_auth_signin
import wordteacher.shared.generated.resources.settings_auth_signout
import wordteacher.shared.generated.resources.settings_auth_title
import wordteacher.shared.generated.resources.settings_frequency_gradation_info_format
import wordteacher.shared.generated.resources.settings_frequency_title
import wordteacher.shared.generated.resources.settings_frequency_upload_file
import wordteacher.shared.generated.resources.settings_hints_reset
import wordteacher.shared.generated.resources.settings_hints_title
import wordteacher.shared.generated.resources.settings_logging_title
import wordteacher.shared.generated.resources.settings_remove_misspelling_db

interface SettingsVM: Clearable {
    var router: SettingsRouter?
    val state: State
    val items: StateFlow<List<BaseViewItem<*>>>
    val eventFlow: Flow<Event>

    fun onSignInClicked(networkType: SpaceAuthService.NetworkType)
    fun onSignOutClicked()
    fun onAuthRefreshClicked()
    fun onUploadWordFrequencyFileClicked()
    fun onLoggingIsEnabledChanged()
    fun onLogFileShareClicked(path: Path)
    fun onEmailClicked()
    fun onPrivacyPolicyClicked()
    fun onResetHintsClicked()
    fun onResetMisspellingDBClicked()

    // Created to use in future
    @Serializable
    data class State(
        val dummyValue: Boolean = false,
    )
}

interface FileSharer {
    suspend fun share(path: Path): Flow<Resource<Unit>>
}

open class SettingsVMImpl (
    restoredState: SettingsVM.State,
    private val authNetworkTypes: Array<SpaceAuthService.NetworkType>,
    private val connectivityManager: ConnectivityManager,
    private val spaceAuthRepository: SpaceAuthRepository,
    private val logsRepository: LogsRepository,
    private val idGenerator: IdGenerator,
    private val isDebug: Boolean,
    private val fileSharer: FileSharer?,
    private val wordFrequencyGradationProvider: WordFrequencyGradationProvider,
    private val wordFrequencyFileOpenController: FileOpenController,
    private val analytics: Analytics,
    private val appInfo: AppInfo,
    private val emailOpener: EmailOpener,
    private val webLinkOpener: WebLinkOpener,
    private val databaseCardWorker: DatabaseCardWorker,
    private val settingStore: SettingStore,
    private val misspellingDBController: FillMisspellingDBController,
    private val symSpellRepository: SymSpellRepository,
): ViewModel(), SettingsVM {

//    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    override var router: SettingsRouter? = null
    final override val state: SettingsVM.State = restoredState

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()
    private val canShareLogs: Boolean
        get() = fileSharer != null

    override val items: StateFlow<List<BaseViewItem<*>>> = combine(
        spaceAuthRepository.authDataFlow,
        logsRepository.isLoggingEnabledState,
        combine(
            wordFrequencyGradationProvider.gradationState,
            wordFrequencyFileOpenController.state
        ) { a, b -> a.downgradeToErrorOrLoading(b) }
    ) { authRes, isLoggingEnabled, gradationState ->
        buildItems(authRes, gradationState, isLoggingEnabled, isDebug)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private fun buildItems(
        authDataRes: Resource<SpaceAuthData>,
        gradationState: Resource<WordFrequencyGradation>,
        isLoggingEnabled: Boolean,
        isDebug: Boolean
    ): List<BaseViewItem<*>> {
        val resultItems: MutableList<BaseViewItem<*>> = mutableListOf()

        resultItems += SettingsViewTitleItem(StringResourceWrapper.String(Res.string.settings_auth_title))
        when(authDataRes) {
            is Resource.Error,
            is Resource.Uninitialized -> {
                resultItems += SettingsViewTextItem(StringResourceWrapper.String(Res.string.settings_auth_signin), withBottomPadding = false)
                resultItems += SettingsSignInItem(authNetworkTypes.toList())
            }
            is Resource.Loaded -> {
                resultItems += SettingsSignOutItem(StringResourceWrapper.FormattedString(Res.string.settings_auth_signout, listOf(authDataRes.data.user.networkType.toString())))
                if (isDebug) {
                    resultItems += SettingsViewAuthRefreshButtonItem(StringResourceWrapper.String(Res.string.settings_auth_refresh))
                }
            }
            is Resource.Loading -> resultItems += SettingsViewLoading()
        }

        resultItems += SettingsOpenDictConfigsItem()
        resultItems += SettingsViewTitleItem(StringResourceWrapper.String(Res.string.settings_frequency_title))
        val wordFrequencyGradationData = gradationState.data()
        if (gradationState.isLoading()) {
            resultItems += SettingsViewLoading()
        } else if (wordFrequencyGradationData != null) {
            resultItems += SettingsViewTextItem(StringResourceWrapper.FormattedString(Res.string.settings_frequency_gradation_info_format, listOf(wordFrequencyGradationData.levels.size + 1)))
            resultItems += SettingsWordFrequencyUploadFileItem(StringResourceWrapper.String(Res.string.settings_frequency_upload_file))
        } else {
            resultItems += SettingsViewTextItem(StringResourceWrapper.String(Res.string.error_default))
            resultItems += SettingsWordFrequencyUploadFileItem(StringResourceWrapper.String(Res.string.settings_frequency_upload_file))
        }

        resultItems += SettingsViewTitleItem(StringResourceWrapper.String(Res.string.settings_logging_title))
        resultItems += SettingsLogsConfigsItem(
            isLoggingEnabled,
            if (canShareLogs) {
                logsRepository.logPaths().map { SettingsLogsConfigsItem.LogFileItem(it) }
            } else {
                emptyList()
            }
        )

        resultItems += SettingsViewTitleItem(StringResourceWrapper.String(Res.string.settings_hints_title))
        resultItems += SettingsResetHintsItem(StringResourceWrapper.String(Res.string.settings_hints_reset))

        if (isDebug) {
            resultItems += SettingsResetMisspellingDBItem(StringResourceWrapper.String(Res.string.settings_remove_misspelling_db))
        }

        resultItems += SettingsPrivacyPolicyItem()
        resultItems += SettingsAbout(
            appTitle = appInfo.getAppInfo(),
            email = appInfo.email
        )

        generateIds(resultItems)
        return resultItems
    }

    private fun generateIds(items: MutableList<BaseViewItem<*>>) {
        generateViewItemIds(items, this.items.value, idGenerator)
    }

    override fun onCleared() {
        super.onCleared()
        eventChannel.cancel()
    }

    override fun onSignInClicked(networkType: SpaceAuthService.NetworkType) {
        analytics.send(AnalyticEvent.createActionEvent("Settings.signInClicked",
            mapOf("networkType" to networkType.name)))
        spaceAuthRepository.launchSignIn(networkType)
    }

    override fun onSignOutClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Settings.signOutClicked"))
        spaceAuthRepository.networkType?.let {
            viewModelScope.launch {
                // TODO: consider extracting this logic in signOut usecase
                databaseCardWorker.waitUntilEditingIsDone()
                databaseCardWorker.waitUntilSyncIsDone()
                spaceAuthRepository.signOut(it)
            }
        }
    }

    override fun onAuthRefreshClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Settings.authRefreshClicked"))
        spaceAuthRepository.launchRefresh()
    }

    override fun onUploadWordFrequencyFileClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Settings.uploadWordFrequencyFileClicked"))
        viewModelScope.launch {
            wordFrequencyFileOpenController.chooseFile()
                .onError {
                    router?.onError(it.toStringResource())
                }
        }
    }

    override fun onLoggingIsEnabledChanged() {
        val newValue = !logsRepository.isLoggingEnabledState.value
        analytics.send(AnalyticEvent.createActionEvent("Settings.loggingIsEnabledChanged",
            mapOf("value" to newValue)))
        logsRepository.setIsLoggingEnabled(newValue)
    }

    override fun onLogFileShareClicked(path: Path) {
        analytics.send(AnalyticEvent.createActionEvent("Settings.logFileShareClicked"))
        viewModelScope.launch {
            fileSharer?.share(path)?.collectUntilDone()
        }
    }

    override fun onEmailClicked() {
        emailOpener.open(appInfo.email)
    }

    override fun onPrivacyPolicyClicked() {
        webLinkOpener.open(appInfo.privacyPolicyUrl)
    }

    override fun onResetHintsClicked() {
        settingStore.resetHint()
    }

    override fun onResetMisspellingDBClicked() {
        viewModelScope.launch {
            symSpellRepository.dictHolder.resetProgress()
            misspellingDBController.reset()
            router?.onError(StringResourceWrapper.Raw("Misspelling db cleared"))
        }
    }
}
