package com.aglushkov.wordteacher.shared.features.settings.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.add_article.vm.ArticleContent
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.FileOpenController
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isError
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradation
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.repository.logs.LogsRepository
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.service.AuthData
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.*
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.ResourceFormatted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Path

interface SettingsVM: Clearable {
    var router: SettingsRouter?
    val state: State
    val items: StateFlow<List<BaseViewItem<*>>>
    val eventFlow: Flow<Event>

    fun restore(newState: State)
    fun onAuthButtonClicked(type: SettingsViewAuthButtonItem.ButtonType)
    fun onAuthRefreshClicked()
    fun onUploadWordFrequencyFileClicked()
    fun onLoggingIsEnabledChanged()
    fun onLogFileShareClicked(path: Path)

    // Created to use in future
    @Parcelize
    class State: Parcelable
}

interface FileSharer {
    fun share(path: Path): Flow<Resource<Unit>>
}

open class SettingsVMImpl (
    override var state: SettingsVM.State,
    private val connectivityManager: ConnectivityManager,
    private val spaceAuthRepository: SpaceAuthRepository,
    private val logsRepository: LogsRepository,
    private val idGenerator: IdGenerator,
    private val isDebug: Boolean,
    private val fileSharer: FileSharer?,
    private val wordFrequencyGradationProvider: WordFrequencyGradationProvider,
    private val wordFrequencyFileOpenController: FileOpenController
): ViewModel(), SettingsVM {

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    override var router: SettingsRouter? = null

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()
    private val canShareLogs: Boolean
        get() = fileSharer != null

    override val items: StateFlow<List<BaseViewItem<*>>> = combine(
        spaceAuthRepository.authDataFlow,
        logsRepository.isLoggingEnabledState,
        wordFrequencyGradationProvider.gradationState,
    ) { authRes, isLoggingEnabled, gradationState ->
        buildItems(authRes, gradationState, isLoggingEnabled, isDebug)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    override fun restore(newState: SettingsVM.State) {
        state = newState
    }

    private fun buildItems(
        authDataRes: Resource<AuthData>,
        gradationState: Resource<WordFrequencyGradation>,
        isLoggingEnabled: Boolean,
        isDebug: Boolean
    ): List<BaseViewItem<*>> {
        val resultItems: MutableList<BaseViewItem<*>> = mutableListOf()

        resultItems += SettingsViewTitleItem(StringDesc.Resource(MR.strings.settings_auth_title))
        resultItems += when(authDataRes) {
            is Resource.Error -> SettingsViewAuthButtonItem(StringDesc.Resource(MR.strings.error_try_again), SettingsViewAuthButtonItem.ButtonType.TryAgain)
            is Resource.Loaded -> SettingsViewAuthButtonItem(StringDesc.Resource(MR.strings.settings_auth_signout), SettingsViewAuthButtonItem.ButtonType.SignOut)
            is Resource.Loading -> SettingsViewLoading()
            is Resource.Uninitialized -> SettingsViewAuthButtonItem(StringDesc.Resource(MR.strings.settings_auth_signin), SettingsViewAuthButtonItem.ButtonType.SignIn)
        }

        if (isDebug) {
            resultItems += SettingsViewAuthRefreshButtonItem(StringDesc.Resource(MR.strings.settings_auth_refresh))
        }

        resultItems += SettingsOpenDictConfigsItem()
        resultItems += SettingsViewTitleItem(StringDesc.Resource(MR.strings.settings_frequency_title))
        val wordFrequencyGradationData = gradationState.data()
        if (wordFrequencyGradationData != null) {
            resultItems += SettingsViewTextItem(StringDesc.ResourceFormatted(MR.strings.settings_frequency_gradation_info_format, wordFrequencyGradationData.levels.size))
            resultItems += SettingsWordFrequencyUploadFileItem(StringDesc.Resource(MR.strings.settings_frequency_upload_file))
        } else if (gradationState.isLoading()) {
            resultItems += SettingsViewLoading()
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

    override fun onAuthButtonClicked(type: SettingsViewAuthButtonItem.ButtonType) {
        when (type) {
            SettingsViewAuthButtonItem.ButtonType.SignIn,
            SettingsViewAuthButtonItem.ButtonType.TryAgain -> spaceAuthRepository.launchSignIn(SpaceAuthService.NetworkType.Google)
            SettingsViewAuthButtonItem.ButtonType.SignOut -> spaceAuthRepository.signOut(SpaceAuthService.NetworkType.Google)
        }
    }

    override fun onAuthRefreshClicked() {
        spaceAuthRepository.launchRefresh()
    }

    override fun onUploadWordFrequencyFileClicked() {
        mainScope.launch {
            wordFrequencyFileOpenController.chooseFile()
        }
    }

    override fun onLoggingIsEnabledChanged() {
        logsRepository.setIsLoggingEnabled(!logsRepository.isLoggingEnabledState.value)
    }

    override fun onLogFileShareClicked(path: Path) {
        mainScope.launch {
            fileSharer?.share(path)?.collect()
        }
    }
}
