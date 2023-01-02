package com.aglushkov.wordteacher.shared.features.settings.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.service.AuthData
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.*
import com.aglushkov.wordteacher.shared.res.MR

interface SettingsVM: Clearable {
    var router: SettingsRouter?
    val state: State
    val items: StateFlow<List<BaseViewItem<*>>>
    val eventFlow: Flow<Event>

    fun restore(newState: State)
    fun onAuthButtonClicked(type: SettingsViewAuthButtonItem.ButtonType)

    // Created to use in future
    @Parcelize
    class State: Parcelable
}

open class SettingsVMImpl (
    override var state: SettingsVM.State,
    private val connectivityManager: ConnectivityManager,
    private val spaceAuthRepository: SpaceAuthRepository,
    private val idGenerator: IdGenerator,
): ViewModel(), SettingsVM {

    override var router: SettingsRouter? = null

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()

    override val items: StateFlow<List<BaseViewItem<*>>> =
        spaceAuthRepository.authDataFlow.map {
            buildAuthItems(it)
        }.stateIn<List<BaseViewItem<*>>>(viewModelScope, SharingStarted.Eagerly, emptyList<BaseViewItem<*>>())

    override fun restore(newState: SettingsVM.State) {
        state = newState
    }

    private fun buildAuthItems(authDataRes: Resource<AuthData>): List<BaseViewItem<*>> {
        val title = SettingsViewTitleItem(StringDesc.Resource(MR.strings.settings_auth_title))
        val button = when(authDataRes) {
            is Resource.Error -> SettingsViewAuthButtonItem(StringDesc.Resource(MR.strings.error_try_again), SettingsViewAuthButtonItem.ButtonType.TryAgain)
            is Resource.Loaded -> SettingsViewAuthButtonItem(StringDesc.Resource(MR.strings.settings_auth_signout), SettingsViewAuthButtonItem.ButtonType.SignOut)
            is Resource.Loading -> SettingsViewLoading()
            is Resource.Uninitialized -> SettingsViewAuthButtonItem(StringDesc.Resource(MR.strings.settings_auth_signin), SettingsViewAuthButtonItem.ButtonType.SignIn)
        }

        return listOf(title, button)
    }

    override fun onCleared() {
        super.onCleared()
        eventChannel.cancel()
    }

    override fun onAuthButtonClicked(type: SettingsViewAuthButtonItem.ButtonType) {
        when (type) {
            SettingsViewAuthButtonItem.ButtonType.SignIn -> router?.openGoogleAuth()
            SettingsViewAuthButtonItem.ButtonType.SignOut -> { TODO("Not Implemented") }
            SettingsViewAuthButtonItem.ButtonType.TryAgain -> router?.openGoogleAuth()
        }
    }
}
