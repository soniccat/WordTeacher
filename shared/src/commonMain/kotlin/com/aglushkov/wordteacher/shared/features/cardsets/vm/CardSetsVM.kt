package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.extensions.addElements
import com.aglushkov.wordteacher.shared.general.extensions.splitBy
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.ShortCardSet
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.cardsetsearch.CardSetSearchRepository
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
    var router: CardSetsRouter?

    val stateFlow: StateFlow<State>
    val eventFlow: Flow<Event>
    val cardSets: StateFlow<Resource<List<BaseViewItem<*>>>>
    val searchCardSets: StateFlow<Resource<List<BaseViewItem<*>>>>
    val availableFeatures: Features

    fun restore(newState: State)
    fun onCardSetAdded(text: String)
    fun onNewCardSetTextChange(text: String)
    fun onStartLearningClicked()
    fun onCardSetClicked(item: CardSetViewItem)
    fun onCardSetRemoved(item: CardSetViewItem)
    fun getErrorText(): StringDesc
    fun getEmptySearchText(): StringDesc
    fun onTryAgainClicked()
    fun onSearch(query: String)
    fun onSearchClosed()
    fun onTryAgainSearchClicked()
    fun onSearchCardSetClicked(item: RemoteCardSetViewItem)
    fun onSearchCardSetAddClicked(item: RemoteCardSetViewItem)
    fun onJsonImportClicked()

    @Parcelize
    data class State (
        val searchQuery: String? = null,
        val newCardSetText: String? = null
    ): Parcelable

    data class Features (
        val canImportCardSetFromJson: Boolean = false
    )
}

open class CardSetsVMImpl(
    state: CardSetsVM.State = CardSetsVM.State(),
    private val cardSetsRepository: CardSetsRepository,
    private val cardSetSearchRepository: CardSetSearchRepository,
    private val timeSource: TimeSource,
    private val idGenerator: IdGenerator,
    override val availableFeatures: CardSetsVM.Features,
): ViewModel(), CardSetsVM {
    override var router: CardSetsRouter? = null

    final override val stateFlow = MutableStateFlow(state)
    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()
    override val cardSets = cardSetsRepository.cardSets.map {
        it.copyWith(buildViewItems(it.data() ?: emptyList(), state.newCardSetText))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    override val searchCardSets = cardSetSearchRepository.cardSets.map {
        it.transform {
            val viewItems = it.map { cardSet ->
                RemoteCardSetViewItem(
                    cardSet.remoteId,
                    cardSet.name,
                    cardSet.terms.take(10),
                ) as BaseViewItem<*>
            }
            generateSearchViewItemIds(viewItems)
            viewItems
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    private fun generateSearchViewItemIds(viewItems: List<BaseViewItem<*>>) {
        generateViewItemIds(viewItems, searchCardSets.value.data().orEmpty(), idGenerator)
    }

    override fun restore(newState: CardSetsVM.State) {
        updateState(newState)
    }

    override fun onCleared() {
        super.onCleared()
        eventChannel.cancel()
    }

    override fun onNewCardSetTextChange(text: String) {
        stateFlow.update { it.copy(newCardSetText = text) }
    }

    override fun onCardSetAdded(name: String) {
        stateFlow.update { it.copy(newCardSetText = null) }

        viewModelScope.launch {
            try {
                cardSetsRepository.createCardSet(name, timeSource.timeInMilliseconds())
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    private fun updateState(newState: CardSetsVM.State) {
        //state = newState
        stateFlow.update { newState }
    }

    fun onCreateTextCardSetClicked() {
        eventChannel.trySend(ShowCreateSetEvent)
    }

    fun onCardSetNameEntered(name: String) {
        viewModelScope.launch {
            cardSetsRepository.createCardSet(name, timeSource.timeInMilliseconds())
        }
    }

    override fun onCardSetClicked(item: CardSetViewItem) {
        router?.openCardSet(item.cardSetId)
    }

    override fun onCardSetRemoved(item: CardSetViewItem) {
        viewModelScope.launch {
            cardSetsRepository.removeCardSet(item.cardSetId)
        }
    }

    override fun onStartLearningClicked() {
        viewModelScope.launch {
            try {
                val allCardIds = cardSetsRepository.allReadyToLearnCardIds()
                router?.openLearning(allCardIds)
            } catch (e: Throwable) {
                // TODO: handle error
            }
        }
    }

    enum class SectionType{
        READY,
        NOT_READY,
        DONE,
    }

    private fun buildViewItems(cardSets: List<ShortCardSet>, newCardSetText: String?): List<BaseViewItem<*>> {
        val groups = cardSets.groupBy {
            if (it.totalProgress < 1.0) {
                if (it.readyToLearnProgress < 1.0) {
                    SectionType.READY
                } else {
                    SectionType.NOT_READY
                }
            } else {
                SectionType.DONE
            }
        }
        val titles = mapOf(
            SectionType.READY to StringDesc.Resource(MR.strings.cardsets_section_ready_to_learn),
            SectionType.NOT_READY to StringDesc.Resource(MR.strings.cardsets_section_not_ready_to_learn),
            SectionType.DONE to StringDesc.Resource(MR.strings.cardsets_section_done),
        )
        val resultList = mutableListOf<BaseViewItem<*>>()
        resultList += CreateCardSetViewItem(
            placeholder = StringDesc.Resource(MR.strings.cardsets_create_cardset),
            text = newCardSetText.orEmpty()
        )

        groups.onEach { e ->
            if (e.value.isNotEmpty()) {
                resultList += SectionViewItem(titles[e.key]!!)
                resultList.addAll(e.value.map(::cardSetViewItem))
            }
        }

        generateViewItemIds(resultList, this.cardSets.value.data().orEmpty(), idGenerator)
        return resultList
    }

    private fun cardSetViewItem(shortCardSet: ShortCardSet): CardSetViewItem {
        return CardSetViewItem(
            shortCardSet.id,
            shortCardSet.name,
            timeSource.stringDate(shortCardSet.creationDate),
            shortCardSet.readyToLearnProgress,
            shortCardSet.totalProgress
        )
    }

    override fun getErrorText(): StringDesc {
        return StringDesc.Resource(MR.strings.cardsets_error)
    }

    override fun getEmptySearchText(): StringDesc {
        return StringDesc.Resource(MR.strings.empty_search_result)
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

    override fun onSearch(query: String) {
        startSearch(query)
    }

    private fun startSearch(query: String) {
        stateFlow.update { it.copy(searchQuery = query) }
        cardSetSearchRepository.search(query)
    }

    override fun onSearchClosed() {
        stateFlow.update { it.copy(searchQuery = null) }
        cardSetSearchRepository.clear()
    }

    override fun onTryAgainSearchClicked() {
        cardSetSearchRepository.search()
    }

    override fun onSearchCardSetClicked(item: RemoteCardSetViewItem) {
    }

    override fun onSearchCardSetAddClicked(item: RemoteCardSetViewItem) {
        cardSetsRepository.addRemoteCardSet(item.remoteCardSetId)
    }

    override fun onJsonImportClicked() {
        router?.openJsonImport()
    }
}

object ShowCreateSetEvent: Event
