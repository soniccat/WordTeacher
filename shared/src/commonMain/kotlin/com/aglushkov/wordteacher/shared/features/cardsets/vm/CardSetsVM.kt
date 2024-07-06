package com.aglushkov.wordteacher.shared.features.cardsets.vm

import androidx.compose.runtime.Stable
import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilDone
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resource.on
import com.aglushkov.wordteacher.shared.general.resource.onError
import com.aglushkov.wordteacher.shared.general.resource.onLoaded
import com.aglushkov.wordteacher.shared.model.ShortCardSet
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.cardsetsearch.CardSetSearchRepository
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface CardSetsVM: Clearable {
    var router: CardSetsRouter?
    val state: State
    val uiStateFlow: StateFlow<UIState>
    val cardSets: StateFlow<Resource<List<BaseViewItem<*>>>>
    val searchCardSets: StateFlow<Resource<List<BaseViewItem<*>>>>
    val availableFeatures: Features

    fun onCardSetAdded(name: String)
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
    fun onEventHandled(event: Event, withAction: Boolean)

    @Serializable
    data class State(
        val searchQuery: String? = null,
        val newCardSetText: String? = null,
    )

    data class UIState(
        val searchQuery: String? = null,
        val newCardSetText: String? = null,
        var openCardSetEvents: List<Event> = emptyList(),
        var loadCardSetErrorEvents: List<Event> = emptyList()
    ) {
        fun toState() = State(
            searchQuery = searchQuery,
            newCardSetText = newCardSetText,
        )
    }

    sealed interface Event {
        val text: StringDesc
        val actionText: StringDesc

        data class OpenCardSetEvent(
            override val text: StringDesc,
            val openText: StringDesc,
            val id: Long,
        ): Event {
            override val actionText: StringDesc
                get() = openText
        }

        data class CardSetLoadingError(
            override val text: StringDesc,
            val remoteId: String,
            val reloadText: StringDesc,
        ): Event {
            override val actionText: StringDesc
                get() = reloadText
        }
    }

    data class Features(
        val canImportCardSetFromJson: Boolean = false
    )
}

open class CardSetsVMImpl(
    restoredState: CardSetsVM.State = CardSetsVM.State(),
    private val cardSetsRepository: CardSetsRepository,
    private val cardSetSearchRepository: CardSetSearchRepository,
    private val timeSource: TimeSource,
    private val idGenerator: IdGenerator,
    override val availableFeatures: CardSetsVM.Features,
    private val analytics: Analytics,
): ViewModel(), CardSetsVM {
    override var router: CardSetsRouter? = null
    final override val state: CardSetsVM.State
        get() { return uiStateFlow.value.toState() }

    final override val uiStateFlow = MutableStateFlow(
        CardSetsVM.UIState(
            searchQuery = restoredState.searchQuery,
            newCardSetText = restoredState.newCardSetText,
        )
    )

    override val cardSets = cardSetsRepository.cardSets.map {
        it.map {
            buildViewItems(it, uiStateFlow.value.newCardSetText)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    override val searchCardSets = cardSetSearchRepository.cardSets.map { cardSetsRes ->
        cardSetsRes.map { cardSets ->
           cardSets.map { searchCardSet ->
               RemoteCardSetViewItem(
                   searchCardSet.cardSet.remoteId,
                   searchCardSet.cardSet.name,
                   searchCardSet.cardSet.terms.take(10),
                   isLoading = searchCardSet.fullCardSetRes.isLoading(),
               ) as BaseViewItem<*>
           }.also {
               generateSearchViewItemIds(it)
           }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    private fun generateSearchViewItemIds(viewItems: List<BaseViewItem<*>>) {
        generateViewItemIds(viewItems, searchCardSets.value.data().orEmpty(), idGenerator)
    }

    override fun onNewCardSetTextChange(text: String) {
        analytics.send(AnalyticEvent.createActionEvent("CardSets.newCardSetTextChange"))
        uiStateFlow.update { it.copy(newCardSetText = text) }
    }

    override fun onCardSetAdded(name: String) {
        analytics.send(AnalyticEvent.createActionEvent("CardSets.cardSetAdded",
            mapOf("name" to name)))
        uiStateFlow.update { it.copy(newCardSetText = null) }

        viewModelScope.launch {
            try {
                cardSetsRepository.createCardSet(name, timeSource.timeInMilliseconds())
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    override fun onCardSetClicked(item: CardSetViewItem) {
        router?.openCardSet(CardSetVM.State.LocalCardSet(item.cardSetId))
    }

    override fun onCardSetRemoved(item: CardSetViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("CardSets.cardSetRemoved",
            mapOf("name" to item.name)))
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
        analytics.send(AnalyticEvent.createActionEvent("CardSets.search",
            mapOf("query" to query)))
        startSearch(query)
    }

    private fun startSearch(query: String) {
        uiStateFlow.update { it.copy(searchQuery = query) }
        cardSetSearchRepository.search(query)
    }

    override fun onSearchClosed() {
        uiStateFlow.update { it.copy(searchQuery = null) }
        cardSetSearchRepository.clear()
    }

    override fun onTryAgainSearchClicked() {
        cardSetSearchRepository.search()
    }

    override fun onSearchCardSetClicked(item: RemoteCardSetViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("CardSets.searchCardSetClicked",
            mapOf("remoteId" to item.remoteCardSetId, "name" to item.name)))
        router?.openCardSet(CardSetVM.State.RemoteCardSet(item.remoteCardSetId))
    }

    override fun onSearchCardSetAddClicked(item: RemoteCardSetViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("CardSets.searchCardSetAddClicked",
            mapOf("remoteId" to item.remoteCardSetId, "name" to item.name)))
        loadCardSetAndAdd(item.remoteCardSetId)
    }

    private fun loadCardSetAndAdd(remoteId: String) {
        viewModelScope.launch {
            cardSetSearchRepository.loadRemoteCardSet(remoteId)
                .waitUntilDone(
                    loaded = { cardSet ->
                        val insertedCardSet = cardSetsRepository.insertCardSet(cardSet)
                        cardSetSearchRepository.removeCardSet(remoteId)
                        uiStateFlow.update {
                            it.copy(
                                openCardSetEvents = it.openCardSetEvents + createOpenCardSetEvent(insertedCardSet.id, insertedCardSet.name)
                            )
                        }
                    },
                    error = { _ ->
                        val cardSet = cardSetSearchRepository.cardSetByRemoteId(remoteId) ?: return@waitUntilDone
                        uiStateFlow.update {
                            it.copy(
                                loadCardSetErrorEvents = it.loadCardSetErrorEvents + createCardSetLoadingErrorEvent(remoteId, cardSet.name)
                            )
                        }
                    },
                )
        }
    }

    private fun createOpenCardSetEvent(id: Long, name: String) = CardSetsVM.Event.OpenCardSetEvent(
        text = StringDesc.ResourceFormatted(MR.strings.cardsets_search_added, name),
        openText = StringDesc.Resource(MR.strings.cardsets_search_added_open),
        id = id,
    )

    private fun createCardSetLoadingErrorEvent(remoteId: String, name: String) = CardSetsVM.Event.CardSetLoadingError(
        text = StringDesc.ResourceFormatted(MR.strings.cardsets_search_added, name),
        remoteId = remoteId,
        reloadText = StringDesc.Resource(MR.strings.cardsets_search_try_again),
    )

    override fun onJsonImportClicked() {
        router?.openJsonImport()
    }

    override fun onEventHandled(event: CardSetsVM.Event, withAction: Boolean) {
        when (event) {
            is CardSetsVM.Event.OpenCardSetEvent -> onOpenCardSetEventHandled(event, withAction)
            is CardSetsVM.Event.CardSetLoadingError -> onCardSetLoadingErrorEventHandled(event, withAction)
        }
    }

    private fun onOpenCardSetEventHandled(
        event: CardSetsVM.Event.OpenCardSetEvent,
        needOpen: Boolean,
    ) {
        uiStateFlow.update {
            it.copy(openCardSetEvents = it.openCardSetEvents.filter { it != event })
        }
        if (needOpen) {
            router?.openCardSet(CardSetVM.State.LocalCardSet(event.id))
        }
    }

    private fun onCardSetLoadingErrorEventHandled(
        event: CardSetsVM.Event.CardSetLoadingError,
        needRetry: Boolean
    ) {
        uiStateFlow.update {
            it.copy(loadCardSetErrorEvents = it.loadCardSetErrorEvents.filter { it != event })
        }
        if (needRetry) {
            loadCardSetAndAdd(event.remoteId)
        }
    }
}
