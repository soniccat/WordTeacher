package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilDone
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoading
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

    @Serializable
    data class State(
        val searchQuery: String? = null,
        val newCardSetText: String? = null,
    )

    data class UIState(
        val searchQuery: String? = null,
        val newCardSetText: String? = null,
    ) {
        fun toState() = State(
            searchQuery = searchQuery,
            newCardSetText = newCardSetText,
        )
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
        it.mapLoadedData {
            buildViewItems(it, uiStateFlow.value.newCardSetText)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    override val searchCardSets = cardSetSearchRepository.cardSets.map { cardSetsRes ->
        cardSetsRes.mapLoadedData { cardSets ->
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
        uiStateFlow.update { it.copy(newCardSetText = text) }
    }

    override fun onCardSetAdded(name: String) {
        analytics.send(
            AnalyticEvent.createActionEvent(
                "CardSets.cardSetAdded",
                mapOf("name" to name)
            )
        )
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
        analytics.send(AnalyticEvent.createActionEvent("CardSets.onStartLearningClicked"))
        router?.openLearning(LearningVM.State(cardSetId = LearningVM.State.AllCards))
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

        var isTopSection = true
        SectionType.entries.toTypedArray().onEach {
            if (groups.containsKey(it)) {
                val items = groups[it].orEmpty().map(::cardSetViewItem)
                if (items.isNotEmpty()) {
                    resultList += SectionViewItem(titles[it]!!, isTopSection)
                    resultList.addAll(items)
                    isTopSection = false
                }
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
            shortCardSet.totalProgress,
            shortCardSet.terms
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

        router?.onError(errorText)
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
                    error = { _ ->
                        val cardSet = cardSetSearchRepository.cardSetByRemoteId(remoteId) ?: return@waitUntilDone
                        router?.onCardSetLoadingError(remoteId, cardSet.name) {
                            loadCardSetAndAdd(remoteId)
                        }
                    },
                    loaded = { cardSet ->
                        val insertedCardSet = cardSetsRepository.insertCardSet(cardSet)
                        cardSetSearchRepository.removeCardSet(remoteId)
                        router?.onCardSetCreated(insertedCardSet.id, insertedCardSet.name)
                    },
                )
        }
    }

    override fun onJsonImportClicked() {
        router?.openJsonImport()
    }
}
