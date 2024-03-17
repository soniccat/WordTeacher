package com.aglushkov.wordteacher.shared.features.cardset_info.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import com.aglushkov.wordteacher.shared.general.resource.onLoaded
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface CardSetInfoVM: Clearable {
    var router: CardSetInfoRouter?
    val state: State
    val uiStateFlow: StateFlow<Resource<UIState>>

    fun onNameChanged(name: String)
    fun onDescriptionChanged(description: String)
    fun onSourceChanged(source: String)
    fun onIsAvailableInSearchChanged(isAvailableInSearch: Boolean)

    @Serializable
    data class State(
        val id: Long,
    )

    data class InputState(
        val name: String? = null,
        val description: String? = null,
        val source: String? = null,
        val isAvailableInSearch: Boolean? = null
    )

    data class UIState(
        val name: String,
        val nameError: StringDesc?,
        val description: String,
        val source: String?,
        val isAvailableInSearch: Boolean
    )
}

open class CardSetInfoVMImpl(
    restoredState: CardSetInfoVM.State,
    private val cardSetRepository: CardSetRepository,
    private val idGenerator: IdGenerator,
): ViewModel(), CardSetInfoVM {
    override var router: CardSetInfoRouter? = null
    final override val state: CardSetInfoVM.State = restoredState

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val cardSetState = MutableStateFlow<Resource<CardSet>>(Resource.Uninitialized())
    private val inputState = MutableStateFlow<CardSetInfoVM.InputState>(CardSetInfoVM.InputState())
    override val uiStateFlow: StateFlow<Resource<CardSetInfoVM.UIState>> = combine(cardSetState, inputState) { cardSetRes, inputState ->
        cardSetRes.transform { cardSet ->
            CardSetInfoVM.UIState(
                name = inputState.name ?: cardSet.name,
                nameError = if (inputState.name?.isEmpty() == true) {
                    StringDesc.Resource(MR.strings.cardset_info_error_empty_name)
                } else {
                    null
                },
                description = inputState.description ?: cardSet.info.description,
                source = inputState.source ?: cardSet.info.source,
                isAvailableInSearch = inputState.isAvailableInSearch ?: cardSet.isAvailableInSearch,
            ).also { _ ->
                syncInputStateWithCardSet(inputState, cardSet)
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, Resource.Uninitialized())

    init {
        scope.launch {
            cardSetRepository.loadCardSetWithoutCards(state.id).collect(cardSetState)
        }
    }

    override fun onNameChanged(name: String) {
        inputState.update { it.copy(name = name) }
    }

    override fun onDescriptionChanged(description: String) {
        inputState.update { it.copy(description = description) }
    }

    override fun onSourceChanged(source: String) {
        inputState.update { it.copy(source = source) }
    }

    override fun onIsAvailableInSearchChanged(isAvailableInSearch: Boolean) {
        inputState.update { it.copy(isAvailableInSearch = isAvailableInSearch) }
    }

    private fun syncInputStateWithCardSet(
        inputState: CardSetInfoVM.InputState,
        cardSet: CardSet
    ) {
        // clear inputState changes if they match cardset
        this.inputState.update {
            it.copy(
                name = if (inputState.name == cardSet.name) {
                    null
                } else {
                    it.name
                },
                description = if (inputState.description == cardSet.info.description) {
                    null
                } else {
                    it.description
                },
                source = if (inputState.source == cardSet.info.source) {
                    null
                } else {
                    it.source
                },
                isAvailableInSearch = if (inputState.isAvailableInSearch == cardSet.isAvailableInSearch) {
                    null
                } else {
                    it.isAvailableInSearch
                },
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}