package com.aglushkov.wordteacher.shared.features.settings.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.add_article.vm.ArticleContent
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
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
    private val fileSharer: FileSharer?
): ViewModel(), SettingsVM {

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    override var router: SettingsRouter? = null

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()
    private val canShareLogs: Boolean
        get() = fileSharer != null

    override val items: StateFlow<List<BaseViewItem<*>>> = combine(
        spaceAuthRepository.authDataFlow,
        logsRepository.isLoggingEnabledState
    ) { authRes, isLoggingEnabled ->
        buildItems(authRes, isLoggingEnabled, isDebug)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    override fun restore(newState: SettingsVM.State) {
        state = newState
    }

    private fun buildItems(
        authDataRes: Resource<AuthData>,
        isLoggingEnabled: Boolean,
        isDebug: Boolean
    ): List<BaseViewItem<*>> {
        val title = SettingsViewTitleItem(StringDesc.Resource(MR.strings.settings_auth_title))
        val button = when(authDataRes) {
            is Resource.Error -> SettingsViewAuthButtonItem(StringDesc.Resource(MR.strings.error_try_again), SettingsViewAuthButtonItem.ButtonType.TryAgain)
            is Resource.Loaded -> SettingsViewAuthButtonItem(StringDesc.Resource(MR.strings.settings_auth_signout), SettingsViewAuthButtonItem.ButtonType.SignOut)
            is Resource.Loading -> SettingsViewLoading()
            is Resource.Uninitialized -> SettingsViewAuthButtonItem(StringDesc.Resource(MR.strings.settings_auth_signin), SettingsViewAuthButtonItem.ButtonType.SignIn)
        }

        val authItems: MutableList<BaseViewItem<*>> = mutableListOf(title, button)
        val resultItems = authItems
        if (isDebug) {
            resultItems += SettingsViewAuthRefreshButtonItem(StringDesc.Raw("Refresh"))
        }

        resultItems += SettingsOpenDictConfigsItem()
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

    override fun onLoggingIsEnabledChanged() {
        logsRepository.setIsLoggingEnabled(!logsRepository.isLoggingEnabledState.value)
    }

    override fun onLogFileShareClicked(path: Path) {
        mainScope.launch {
            fileSharer?.share(path)?.collect()
        }
    }
}
