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
import com.aglushkov.wordteacher.shared.general.getAppInfo
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.downgradeToErrorOrLoading
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradation
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.repository.logs.LogsRepository
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.service.SpaceAuthData
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.*
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toBlockingSettings
import dev.icerock.moko.resources.desc.ResourceFormatted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Path
import kotlinx.serialization.Serializable

interface SettingsVM: Clearable {
    var router: SettingsRouter?
    val state: State
    val items: StateFlow<List<BaseViewItem<*>>>
    val eventFlow: Flow<Event>

    fun onSignInClicked(networkType: SpaceAuthService.NetworkType)
    fun onSignOutClicked()
    fun onAuthRefreshClicked()
    fun onUploadWordFrequencyFileClicked()
    fun onGetWordFromClipboardChanged(newValue: Boolean)
    fun onLoggingIsEnabledChanged()
    fun onLogFileShareClicked(path: Path)
    fun onEmailClicked()
    fun onPrivacyPolicyClicked()

    // Created to use in future
    @Serializable
    data class State(
        val dummyValue: Boolean = false,
    )
}

interface FileSharer {
    fun share(path: Path): Flow<Resource<Unit>>
}

open class SettingsVMImpl (
    restoredState: SettingsVM.State,
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
    private val settings: FlowSettings,
): ViewModel(), SettingsVM {

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    override var router: SettingsRouter? = null
    final override val state: SettingsVM.State = restoredState

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()
    private val canShareLogs: Boolean
        get() = fileSharer != null

    override val items: StateFlow<List<BaseViewItem<*>>> = combine(
        spaceAuthRepository.authDataFlow,
        settings.getBooleanFlow(SETTING_GET_WORD_FROM_CLIPBOARD, false),
        logsRepository.isLoggingEnabledState,
        combine(
            wordFrequencyGradationProvider.gradationState,
            wordFrequencyFileOpenController.state
        ) { a, b -> a.downgradeToErrorOrLoading(b) }
    ) { authRes, isGetWordFromClipboardEnabled, isLoggingEnabled, gradationState ->
        buildItems(authRes, gradationState, isGetWordFromClipboardEnabled, isLoggingEnabled, isDebug)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun buildItems(
        authDataRes: Resource<SpaceAuthData>,
        gradationState: Resource<WordFrequencyGradation>,
        isGetWordFromClipboardEnabled: Boolean,
        isLoggingEnabled: Boolean,
        isDebug: Boolean
    ): List<BaseViewItem<*>> {
        val resultItems: MutableList<BaseViewItem<*>> = mutableListOf()

        resultItems += SettingsViewTitleItem(StringDesc.Resource(MR.strings.settings_auth_title))
        when(authDataRes) {
            is Resource.Error,
            is Resource.Uninitialized -> {
                resultItems += SettingsViewTextItem(StringDesc.Resource(MR.strings.settings_auth_signin), withBottomPadding = false)
                resultItems += SettingsSignInItem(
                    listOf(SpaceAuthService.NetworkType.YandexId, SpaceAuthService.NetworkType.VKID, SpaceAuthService.NetworkType.Google)
                )
            }
            is Resource.Loaded -> {
                resultItems += SettingsSignOutItem(StringDesc.ResourceFormatted(MR.strings.settings_auth_signout, authDataRes.data.user.networkType.toString()))
                if (isDebug) {
                    resultItems += SettingsViewAuthRefreshButtonItem(StringDesc.Resource(MR.strings.settings_auth_refresh))
                }
            }
            is Resource.Loading -> resultItems += SettingsViewLoading()
        }

        resultItems += SettingsViewTitleItem(StringDesc.Resource(MR.strings.settings_words_title))
        resultItems += SettingsWordsUseClipboardItem(isGetWordFromClipboardEnabled)

        resultItems += SettingsOpenDictConfigsItem()
        resultItems += SettingsViewTitleItem(StringDesc.Resource(MR.strings.settings_frequency_title))
        val wordFrequencyGradationData = gradationState.data()
        if (gradationState.isLoading()) {
            resultItems += SettingsViewLoading()
        } else if (wordFrequencyGradationData != null) {
            resultItems += SettingsViewTextItem(StringDesc.ResourceFormatted(MR.strings.settings_frequency_gradation_info_format, wordFrequencyGradationData.levels.size + 1))
            resultItems += SettingsWordFrequencyUploadFileItem(StringDesc.Resource(MR.strings.settings_frequency_upload_file))
        } else {
            resultItems += SettingsViewTextItem(StringDesc.Resource(MR.strings.error_default))
            resultItems += SettingsWordFrequencyUploadFileItem(StringDesc.Resource(MR.strings.settings_frequency_upload_file))
        }

        resultItems += SettingsViewTitleItem(StringDesc.Resource(MR.strings.settings_logging_title))
        resultItems += SettingsLogsConfigsItem(
            isLoggingEnabled,
            if (canShareLogs) {
                logsRepository.logPaths().map { SettingsLogsConfigsItem.LogFileItem(it) }
            } else {
                emptyList()
            }
        )
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
            mainScope.launch {
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
        mainScope.launch {
            wordFrequencyFileOpenController.chooseFile()
        }
    }

    override fun onGetWordFromClipboardChanged(newValue: Boolean) {
        settings.toBlockingSettings().putBoolean(SETTING_GET_WORD_FROM_CLIPBOARD, newValue)
    }

    override fun onLoggingIsEnabledChanged() {
        val newValue = !logsRepository.isLoggingEnabledState.value
        analytics.send(AnalyticEvent.createActionEvent("Settings.loggingIsEnabledChanged",
            mapOf("value" to newValue)))
        logsRepository.setIsLoggingEnabled(newValue)
    }

    override fun onLogFileShareClicked(path: Path) {
        analytics.send(AnalyticEvent.createActionEvent("Settings.logFileShareClicked"))
        mainScope.launch {
            fileSharer?.share(path)?.collect()
        }
    }

    override fun onEmailClicked() {
        emailOpener.open(appInfo.email)
    }

    override fun onPrivacyPolicyClicked() {
        webLinkOpener.open(appInfo.privacyPolicyUrl)
    }
}

const val SETTING_GET_WORD_FROM_CLIPBOARD = "getWordFromClipboard"