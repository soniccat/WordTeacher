package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.ShortCardSet
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface CardSetsVM: Clearable {
    val stateFlow: StateFlow<State>
    val eventFlow: Flow<Event>
    val cardSets: StateFlow<Resource<List<BaseViewItem<*>>>>

    fun restore(newState: State)
    fun onCardSetAdded(text: String)
    fun onNewCardSetTextChange(text: String)
    fun onStartLearningClicked()
    fun onCardSetClicked(item: CardSetViewItem)
    fun onCardSetRemoved(item: CardSetViewItem)
    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?
    fun onTryAgainClicked()

    @Parcelize
    data class State (
        val newCardSetText: String? = null
    ): Parcelable
}

open class CardSetsVMImpl(
    var state: CardSetsVM.State = CardSetsVM.State(),
    val cardSetsRepository: CardSetsRepository,
    private val router: CardSetsRouter,
    private val timeSource: TimeSource
): ViewModel(), CardSetsVM {

    final override val stateFlow = MutableStateFlow(state)
    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()
    override val cardSets = cardSetsRepository.cardSets.map {
        Logger.v("build view items")
        it.copyWith(buildViewItems(it.data() ?: emptyList(), state.newCardSetText))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    override fun restore(newState: CardSetsVM.State) {
        state = newState
    }

    override fun onCleared() {
        super.onCleared()
        eventChannel.cancel()
    }

    override fun onNewCardSetTextChange(text: String) {
        updateState(state.copy(newCardSetText = text))
    }

    override fun onCardSetAdded(name: String) {
        updateState(state.copy(newCardSetText = null))

        viewModelScope.launch {
            try {
                cardSetsRepository.createCardSet(name, timeSource.getTimeInMilliseconds())
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    private fun updateState(newState: CardSetsVM.State) {
        state = newState
        stateFlow.value = state
    }

    fun onCreateTextCardSetClicked() {
        eventChannel.trySend(ShowCreateSetEvent)
    }

    fun onCardSetNameEntered(name: String) {
        viewModelScope.launch {
            cardSetsRepository.createCardSet(name, timeSource.getTimeInMilliseconds())
        }
    }

    override fun onCardSetClicked(item: CardSetViewItem) {
        router.openCardSet(item.id)
    }

    override fun onCardSetRemoved(item: CardSetViewItem) {
        viewModelScope.launch {
            cardSetsRepository.removeCardSet(item.id)
        }
    }

    override fun onStartLearningClicked() {
        viewModelScope.launch {
            try {
                val allCardIds = cardSetsRepository.allCardIds()
                router.openLearning(allCardIds)
            } catch (e: Throwable) {
                // TODO: handle error
            }
        }
    }

    private fun buildViewItems(cardSets: List<ShortCardSet>, newCardSetText: String?): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()

        cardSets.forEach {
            items.add(CardSetViewItem(it.id, it.name, timeSource.stringDate(it.date)))
        }

        return listOf(
            CreateCardSetViewItem(
                placeholder = StringDesc.Resource(MR.strings.cardsets_create_cardset),
                text = newCardSetText.orEmpty()
            ),
            *items.toTypedArray())
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.cardsets_error)
    }

    override fun onTryAgainClicked() {
        // TODO: do sth with articlesRepository
    }

    private fun showError(e: Exception) {
        val errorText = e.message?.let {
            StringDesc.Raw(it)
        } ?: StringDesc.Resource(MR.strings.error_default)

        // TODO: pass an error message
        //eventChannel.offer(ErrorEvent(errorText))
    }
}

object ShowCreateSetEvent: Event
