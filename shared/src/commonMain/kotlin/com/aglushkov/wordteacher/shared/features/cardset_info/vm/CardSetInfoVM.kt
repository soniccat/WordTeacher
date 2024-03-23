package com.aglushkov.wordteacher.shared.features.cardset_info.vm

import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.StringDescThrowable
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.CardSet
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
import kotlinx.serialization.Serializable
import java.lang.RuntimeException

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
    data class State(
        val id: Long,
    )

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
        val isAvailableInSearch: Boolean
    )
}

open class CardSetInfoVMImpl(
    restoredState: CardSetInfoVM.State,
    private val databaseCardWorker: DatabaseCardWorker,
    private val cardSetRepository: CardSetRepository,
    private val idGenerator: IdGenerator,
): ViewModel(), CardSetInfoVM {
    override var router: CardSetInfoRouter? = null
    final override val state: CardSetInfoVM.State = restoredState

    private val cardSetState = MutableStateFlow<Resource<CardSet>>(Resource.Uninitialized())
    private val inputState = MutableStateFlow<CardSetInfoVM.InputState>(CardSetInfoVM.InputState())
    override val uiStateFlow: StateFlow<Resource<CardSetInfoVM.UIState>> = combine(
        combine(cardSetState, databaseCardWorker.untilFirstEditingFlow()) { cardSetState, workerState ->
            if (workerState == DatabaseCardWorker.State.EDITING) {
                cardSetState
            } else {
                Resource.Loading()
            }
        },
        inputState,
    ) { cardSetRes, inputState ->
        cardSetRes.transform(
            errorTransformer = {
                StringDescThrowable(ResourceStringDesc(MR.strings.cardset_info_error), it)
            }
        ) { cardSet ->
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
            ).also { _ ->
//                syncInputStateWithCardSet(inputState, cardSet)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    init {
        if (databaseCardWorker.currentState != DatabaseCardWorker.State.EDITING) {
            addClearable(databaseCardWorker.startEditing())
        }
        loadCardSet()

        // subscribe on card set db state to render
        viewModelScope.launch {
            cardSetRepository.cardSetWithoutCardsFlow(state.id).collect(cardSetState)
        }

        // subscribe on input state to update db data
        viewModelScope.launch {
            inputState.collect { lastInputState ->
                cardSetState.value.data()?.let { dbCardSet ->
                    databaseCardWorker.updateCardSetInfo(
                        dbCardSet.copy(
                            name = lastInputState.name ?: dbCardSet.name,
                            info = dbCardSet.info.copy(
                                description = lastInputState.description ?: dbCardSet.info.description,
                                source = lastInputState.source ?: dbCardSet.info.source,
                            ),
                            isAvailableInSearch = lastInputState.isAvailableInSearch ?: dbCardSet.isAvailableInSearch
                        )
                    )
                }
            }
        }
    }

    override fun onTryAgainPressed() {
        loadCardSet()
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

    private fun loadCardSet() {
        viewModelScope.launch(Dispatchers.Default) {
            cardSetRepository.loadCardSetWithoutCards(state.id).collect(cardSetState)
        }
    }

//    private fun syncInputStateWithCardSet(
//        inputState: CardSetInfoVM.InputState,
//        cardSet: CardSet
//    ) {
//        // clear inputState changes if they match cardset
//        this.inputState.update {
//            it.copy(
//                name = if (inputState.name == cardSet.name) {
//                    null
//                } else {
//                    it.name
//                },
//                description = if (inputState.description == cardSet.info.description) {
//                    null
//                } else {
//                    it.description
//                },
//                source = if (inputState.source == cardSet.info.source) {
//                    null
//                } else {
//                    it.source
//                },
//                isAvailableInSearch = if (inputState.isAvailableInSearch == cardSet.isAvailableInSearch) {
//                    null
//                } else {
//                    it.isAvailableInSearch
//                },
//            )
//        }
//    }
}