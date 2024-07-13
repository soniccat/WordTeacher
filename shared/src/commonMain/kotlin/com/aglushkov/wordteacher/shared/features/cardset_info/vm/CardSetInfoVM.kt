package com.aglushkov.wordteacher.shared.features.cardset_info.vm

import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.StringDescThrowable
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.CardSetInfo
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceStringDesc
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

interface CardSetInfoVM: Clearable {
    var router: CardSetInfoRouter?
    val state: State
    val uiStateFlow: StateFlow<Resource<UIState>>

    fun onTryAgainPressed()
    fun onNameChanged(name: String)
    fun onDescriptionChanged(description: String)
    fun onSourceChanged(source: String)
    fun onIsAvailableInSearchChanged(isAvailableInSearch: Boolean)

    @Serializable
    sealed interface State {
        val isRemoteCardSet: Boolean

        @Serializable
        data class LocalCardSet(
            val id: Long
        ) : State {
            override val isRemoteCardSet: Boolean = false
        }
        @Serializable
        data class RemoteCardSet(
            val cardSet: CardSet,
        ) : State {
            override val isRemoteCardSet: Boolean = true
        }
    }

    data class InputState(
        val name: String? = null,
        val description: String? = null,
        val source: String? = null,
        val isAvailableInSearch: Boolean? = null
    ) {
        val isNameValid: Boolean
            get() = name == null || name.isNotEmpty()
        val validatedName: String?
            get() = if (isNameValid) {
                name
            } else {
                null
            }
    }

    data class UIState(
        val name: String,
        val nameError: StringDesc?,
        val description: String,
        val source: String?,
        val isAvailableInSearch: Boolean,
        val isEditable: Boolean,
    )
}

open class CardSetInfoVMImpl(
    restoredState: CardSetInfoVM.State,
    private val databaseCardWorker: DatabaseCardWorker,
    private val cardSetRepository: CardSetRepository,
    private val analytics: Analytics,
): ViewModel(), CardSetInfoVM {

    override var router: CardSetInfoRouter? = null
    final override val state: CardSetInfoVM.State = restoredState

    private val cardSetState = MutableStateFlow<Resource<CardSet>>(Resource.Uninitialized())
    private val inputState = MutableStateFlow(CardSetInfoVM.InputState())
    override val uiStateFlow: StateFlow<Resource<CardSetInfoVM.UIState>> = combine(
        if (state.isRemoteCardSet) {
            cardSetState
        } else {
            combine(
                cardSetState,
                databaseCardWorker.untilFirstEditingFlow()
            ) { cardSetState, workerState ->
                if (workerState == DatabaseCardWorker.State.EDITING) {
                    cardSetState
                } else {
                    Resource.Loading()
                }
            }
        },
        inputState,
    ) { cardSetRes, inputState ->
        cardSetRes.mapLoadedData(
            errorTransformer = {
                StringDescThrowable(ResourceStringDesc(MR.strings.cardset_info_error), it)
            }
        ) { cardSet: CardSet ->
            CardSetInfoVM.UIState(
                name = inputState.validatedName ?: cardSet.name,
                nameError = if (inputState.isNameValid) {
                    null
                } else {
                    StringDesc.Resource(MR.strings.cardset_info_error_empty_name)
                },
                description = inputState.description ?: cardSet.info.description,
                source = inputState.source ?: cardSet.info.source,
                isAvailableInSearch = inputState.isAvailableInSearch ?: cardSet.isAvailableInSearch,
                isEditable = !state.isRemoteCardSet
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    init {
        addClearable(databaseCardWorker.startEditing())
        loadCardSet()

        if (!state.isRemoteCardSet) {
            // subscribe on input state to update db data
            viewModelScope.launch {
                inputState.collect { lastInputState ->
                    cardSetState.value.data()?.let { dbCardSet ->
                        databaseCardWorker.updateCardSetInfo(
                            dbCardSet.copy(
                                name = lastInputState.validatedName ?: dbCardSet.name,
                                info = dbCardSet.info.copy(
                                    description = lastInputState.description
                                        ?: dbCardSet.info.description,
                                    source = lastInputState.source ?: dbCardSet.info.source,
                                ),
                                isAvailableInSearch = lastInputState.isAvailableInSearch
                                    ?: dbCardSet.isAvailableInSearch
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onTryAgainPressed() {
        loadCardSet()
    }

    override fun onNameChanged(name: String) {
        logChange("name")
        inputState.update { it.copy(name = name) }
    }

    override fun onDescriptionChanged(description: String) {
        logChange("description")
        inputState.update { it.copy(description = description) }
    }

    override fun onSourceChanged(source: String) {
        logChange("source")
        inputState.update { it.copy(source = source) }
    }

    override fun onIsAvailableInSearchChanged(isAvailableInSearch: Boolean) {
        logChange("isAvailableInSearchChanged")
        inputState.update { it.copy(isAvailableInSearch = isAvailableInSearch) }
    }

    private fun logChange(fieldType: String) {
        analytics.send(AnalyticEvent.createActionEvent("CardSetInfo.change", mapOf("fieldType" to fieldType)))
    }

    private fun loadCardSet() {
        viewModelScope.launch(Dispatchers.Default) {
            when (state) {
                is CardSetInfoVM.State.LocalCardSet -> {
                    cardSetRepository.loadCardSetWithoutCards(state.id).collect(cardSetState)
                }
                is CardSetInfoVM.State.RemoteCardSet -> {
                    cardSetState.value = Resource.Loaded(
                        CardSet(
                            id = 0,
                            remoteId = "",
                            name = state.cardSet.name,
                            creationDate = Instant.DISTANT_PAST,
                            modificationDate = Instant.DISTANT_PAST,
                            cards = listOf(),
                            terms = listOf(),
                            creationId = "",
                            info = CardSetInfo(
                                description = state.cardSet.info.description,
                                source = state.cardSet.info.source
                            ),
                            isAvailableInSearch = false
                        )
                    )
                }
            }
        }
    }
}
