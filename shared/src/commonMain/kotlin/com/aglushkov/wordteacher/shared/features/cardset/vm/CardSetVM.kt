package com.aglushkov.wordteacher.shared.features.cardset.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.events.FocusViewItemEvent
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.*
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradation
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.workers.UPDATE_DELAY
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

interface CardSetVM: Clearable {
    var router: CardSetRouter?
    val state: StateFlow<State>
    val cardSet: StateFlow<Resource<out CardSet>>
    val viewItems: StateFlow<Resource<List<BaseViewItem<*>>>>
    val eventFlow: Flow<List<Event>>

    fun onCardCreatePressed()
    fun onCardDeleted(cardId: Long)
    fun onPartOfSpeechChanged(newPartOfSpeech: WordTeacherWord.PartOfSpeech, cardId: Long)
    fun onItemTextChanged(text: String, item: BaseViewItem<*>, cardId: Long)
    fun onAddDefinitionPressed(cardId: Long)
    fun onDefinitionRemoved(item: WordDefinitionViewItem, cardId: Long)
    fun onAddExamplePressed(cardId: Long)
    fun onExampleRemoved(item: WordExampleViewItem, cardId: Long)
    fun onAddSynonymPressed(cardId: Long)
    fun onSynonymRemoved(item: WordSynonymViewItem, cardId: Long)
    fun onBackPressed()
    fun onTryAgainClicked()
    fun onInfoPressed()
    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?
    fun getPlaceholder(viewItem: BaseViewItem<*>): StringDesc?
    fun onStartLearningClicked()
    fun onAddClicked()

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
            val id: String
        ) : State {
            override val isRemoteCardSet: Boolean = true
        }
    }
}

open class CardSetVMImpl(
    restoredState: CardSetVM.State,
    private val cardSetsRepository: CardSetsRepository,
    private val cardSetRepository: CardSetRepository,
    wordFrequencyGradationProvider: WordFrequencyGradationProvider,
    private val databaseCardWorker: DatabaseCardWorker,
    private val timeSource: TimeSource,
    private val idGenerator: IdGenerator
): ViewModel(), CardSetVM {

    override var router: CardSetRouter? = null
    final override var state = MutableStateFlow(restoredState)

    private var pendingEvents = mutableListOf<PendingEvent>()
    private val notHandledPendingEvents: List<PendingEvent>
        get() {
            pendingEvents.removeAll { it.isHandled }
            return pendingEvents
        }

    // Contains cards being edited and haven't been synched with DB
    private var inMemoryCardSet = MutableStateFlow<CardSet?>(null)
    final override val cardSet: StateFlow<Resource<CardSet>> = combine(
        cardSetRepository.cardSet, inMemoryCardSet, databaseCardWorker.untilFirstEditingFlow(),
        transform = { cardSet, inMemoryCardSet, state ->
            if (state == DatabaseCardWorker.State.EDITING) {
                if (inMemoryCardSet == null) {
                    cardSet
                } else {
                    when (cardSet) {
                        is Resource.Loaded -> cardSet.copyWith(
                            mergeCardSets(
                                cardSet.data,
                                inMemoryCardSet
                            )
                        ).also {
                            pruneInMemoryCardSet(cardSet.data)
                        }
                        else -> cardSet
                    }
                }
            } else {
                Resource.Loading()
            }
    }).stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    private val events = MutableStateFlow<List<Event>>(emptyList())
    override val eventFlow = events.map { eventList -> eventList.filter { !it.isHandled } }

    override val viewItems = combine(
        cardSet, wordFrequencyGradationProvider.gradationState,
        transform = { cardSet, gradation ->
            //Logger.v("build view items")
            cardSet.copyWith(buildViewItems(cardSet, gradation))
        }
    ).stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    init {
        addClearable(databaseCardWorker.startEditing())

        val safeState = state.value
        viewModelScope.launch {
            when (safeState) {
                is CardSetVM.State.RemoteCardSet -> {
                    cardSetRepository.loadRemoteCardSet(safeState.id)
                }
                is CardSetVM.State.LocalCardSet -> {
                    cardSetRepository.loadAndObserveCardSet(safeState.id)
                }
            }
        }
    }

    private fun buildViewItems(cardSetRes: Resource<out CardSet>, frequencyGradationRes: Resource<WordFrequencyGradation>): List<BaseViewItem<*>> {
        return when (cardSetRes) {
            is Resource.Loaded -> {
                makeViewItems(cardSetRes.data, frequencyGradationRes.data())
            }
            else -> emptyList()
        }
    }

    private fun mergeCardSets(dbCardSet: CardSet, inMemoryCardSet: CardSet): CardSet {
        // replace db cards with in-memory cards
        //Logger.v("merge cardSet $dbCardSet\n with $inMemoryCardSet")

        return dbCardSet.copy(
            cards = dbCardSet.cards.map {
                inMemoryCardSet.findCard(it.id) ?: it
            }
        )
    }

    private fun pruneInMemoryCardSet(dbCardSet: CardSet) {
        // remove equal sets from inMemoryCardSet
        inMemoryCardSet.update {
            it?.copy(
                cards = it.cards.filter { inMemoryCard ->
                    dbCardSet.findCard(inMemoryCard.id) != inMemoryCard
                }
            )
        }
    }

    private fun makeViewItems(loadedCardSet: CardSet, frequencyGradation: WordFrequencyGradation?): List<BaseViewItem<*>>  {
        val result = mutableListOf<BaseViewItem<*>>()

        loadedCardSet.cards.onEach { card ->
            var lastDefViewItem: BaseViewItem<*>? = null
            var lastExViewItem: BaseViewItem<*>? = null
            var lastSynViewItem: BaseViewItem<*>? = null

            val cardViewItems = mutableListOf<BaseViewItem<*>>()
            val gradationLevelAndRatio = frequencyGradation?.gradationLevelAndRatio(card.termFrequency)
            cardViewItems += WordTitleViewItem(
                card.term,
                providers = emptyList(),
                cardId = card.id,
                frequencyLevelAndRatio = gradationLevelAndRatio,
            )
            cardViewItems += WordTranscriptionViewItem(card.transcription.orEmpty(), cardId = card.id)
            cardViewItems += WordPartOfSpeechViewItem(card.partOfSpeech.toStringDesc(), card.partOfSpeech, cardId = card.id)

            if (card.definitions.isEmpty()) {
                cardViewItems += WordDefinitionViewItem("", index = 0, isLast = true, cardId = card.id)
            } else {
                card.definitions.onEachIndexed { index, def ->
                    cardViewItems += WordDefinitionViewItem(def, index = index, isLast = index == card.definitions.size - 1, cardId = card.id).also {
                        lastDefViewItem = it
                    }
                }
            }

            // Examples
            cardViewItems += WordSubHeaderViewItem(
                StringDesc.Resource(MR.strings.word_section_examples),
                Indent.SMALL,
                isOnlyHeader = card.examples.isEmpty(),
                contentType = WordSubHeaderViewItem.ContentType.EXAMPLES,
                cardId = card.id
            )

            card.examples.onEachIndexed { index, example ->
                cardViewItems += WordExampleViewItem(example, Indent.SMALL, index, isLast = index == card.examples.size - 1, cardId = card.id).also {
                    lastExViewItem = it
                }
            }

            // Synonyms
            cardViewItems += WordSubHeaderViewItem(
                StringDesc.Resource(MR.strings.word_section_synonyms),
                Indent.SMALL,
                isOnlyHeader = card.synonyms.isEmpty(),
                contentType = WordSubHeaderViewItem.ContentType.SYNONYMS,
                cardId = card.id
            )

            card.synonyms.onEachIndexed { index, synonym ->
                cardViewItems += WordSynonymViewItem(synonym, Indent.SMALL, index, isLast = index == card.synonyms.size - 1, cardId = card.id).also {
                    lastSynViewItem = it
                }
            }

            result += cardViewItems
            result += WordDividerViewItem()

            makeFocusEvents(card.id, lastDefViewItem, lastExViewItem, lastSynViewItem)
        }

        result += CreateCardViewItem()
        generateIds(result)

        return result
    }

    private fun makeFocusEvents(cardId: Long, lastDefViewItem: BaseViewItem<*>?, lastExViewItem: BaseViewItem<*>?, lastSynViewItem: BaseViewItem<*>?) {
        val filteredEvents = this.events.value.filter { it !is FocusViewItemEvent }
        this.notHandledPendingEvents.onEach {
            when (val e = it) {
                is PendingEvent.FocusLast -> {
                    lastDefViewItem?.let { safeItem ->
                        if (e.type == FocusLastType.Definition && e.cardId == cardId) {
                            events.value = filteredEvents + e.makeEvent(safeItem)
                        }
                    }
                    lastExViewItem?.let { safeItem ->
                        if (e.type == FocusLastType.Example && e.cardId == cardId) {
                            events.value = filteredEvents + e.makeEvent(safeItem)
                        }
                    }
                    lastSynViewItem?.let { safeItem ->
                        if (e.type == FocusLastType.Synonym && e.cardId == cardId) {
                            events.value = filteredEvents + e.makeEvent(safeItem)
                        }
                    }
                }
            }
        }
    }

    private fun generateIds(items: MutableList<BaseViewItem<*>>) {
        val prevItems = viewItems.value.data().orEmpty()
        if (items.size == prevItems.size) { // keep ids not to recompose text fields being edited
            prevItems.onEachIndexed { index, baseViewItem ->
                items[index].id = baseViewItem.id
            }
        } else { // regenerate not to mix up with current ids where swipeable cells are in hidden state
            items.onEach {
                it.id = idGenerator.nextId()
            }
        }
    }

    override fun onTryAgainClicked() {
        // TODO: do sth with articlesRepository
    }

    override fun onInfoPressed() {
        val safeState = state.value
        when (safeState) {
            is CardSetVM.State.LocalCardSet -> CardSetInfoVM.State.LocalCardSet(
                id = safeState.id
            )
            is CardSetVM.State.RemoteCardSet -> cardSetRepository.cardSet.value.data()?.let {
                CardSetInfoVM.State.RemoteCardSet(
                    cardSet = it
                )
            }
        }?.let {
            router?.openCardSetInfo(it)
        }
    }

    override fun onItemTextChanged(text: String, item: BaseViewItem<*>, cardId: Long) {
        editCard(cardId) { card ->
            val newCard = when (item) {
                is WordTitleViewItem ->
                    card.copy(
                        term = if (card.term != text) {
                            text
                        } else {
                            card.term
                        },
                    )
                is WordTranscriptionViewItem ->
                    card.copy(
                        transcription = if (card.transcription != text) {
                            text
                        } else {
                            card.transcription
                        },
                    )
                is WordDefinitionViewItem -> {
                    card.copy(
                        definitions = if (card.definitions.isEmpty()) {
                            listOf(text)
                        } else {
                            card.definitions.mapIndexed { index, s ->
                                if (item.index == index) {
                                    text
                                } else {
                                    s
                                }
                            }
                        },
                        needToUpdateDefinitionSpans = true,
                    )
                }
                is WordExampleViewItem ->
                    card.copy(
                        examples = card.examples.mapIndexed { index, s ->
                            if (item.index == index) {
                                text
                            } else {
                                s
                            }
                        },
                        needToUpdateExampleSpans = true,
                    )
                is WordSynonymViewItem ->
                    card.copy(
                        synonyms = card.synonyms.mapIndexed { index, s ->
                            if (item.index == index) {
                                text
                            } else {
                                s
                            }
                        },
                    )
                else -> card
            }

            if (newCard != card) {
                updateCard(newCard)
            }
            newCard
        }
    }

    private fun editCard(cardId: Long, transform: (card: Card) -> Card) {
        val cardSet = obtainInMemoryCardSet(cardId) ?: return
        inMemoryCardSet.update {
            cardSet.copy(
                cards = cardSet.cards.map {
                    if (it.id == cardId) {
                        transform(it)
                    } else {
                        it
                    }
                },
            )
        }
    }

    private fun obtainInMemoryCardSet(cardId: Long): CardSet? {
        val resultSet = inMemoryCardSet.value ?: cardSet.value.data()?.copy(cards = emptyList()) ?: return null
        val inMemoryCard = inMemoryCardSet.value?.findCard(cardId)
        val modifiedSet = if (inMemoryCard != null) {
            resultSet
        } else {
            val cardSetCard = cardSet.value.data()?.findCard(cardId) ?: return null
            resultSet.copy(
                cards = resultSet.cards + cardSetCard
            )
        }

        inMemoryCardSet.update { modifiedSet }
        return modifiedSet
    }

    override fun onAddDefinitionPressed(cardId: Long) {
        pendingEvents.add(PendingEvent.FocusLast(FocusLastType.Definition, cardId))
        return editCard(cardId) {
            it.copy(
                definitions = if (it.definitions.lastOrNull() != "") {
                    it.definitions + ""
                } else {
                    it.definitions
                },
            )
        }
    }

    override fun onDefinitionRemoved(item: WordDefinitionViewItem, cardId: Long) =
        editCard(cardId) {
            it.copy(
                definitions = if (it.definitions.size <= 1) {
                    listOf("")
                } else {
                    it.definitions.filterIndexed { i, _ -> i != item.index }
                },
                needToUpdateDefinitionSpans = true,
            ).apply {
                updateCard(this, delay = 0)
            }
        }

    override fun onAddExamplePressed(cardId: Long) {
        pendingEvents.add(PendingEvent.FocusLast(FocusLastType.Example, cardId))
        return editCard(cardId) {
            it.copy(
                examples = if (it.examples.lastOrNull() != "") {
                    it.examples + ""
                } else {
                    it.examples
                },
            )
        }
    }

    override fun onExampleRemoved(item: WordExampleViewItem, cardId: Long) =
        editCard(cardId) {
            it.copy(
                examples = it.examples.filterIndexed { i, _ -> i != item.index },
                needToUpdateExampleSpans = true,
            ).apply {
                updateCard(this, delay = 0)
            }
        }

    override fun onAddSynonymPressed(cardId: Long) {
        pendingEvents.add(PendingEvent.FocusLast(FocusLastType.Synonym, cardId))
        editCard(cardId) {
            it.copy(
                synonyms = if (it.synonyms.lastOrNull() != "") {
                    it.synonyms + ""
                } else {
                    it.synonyms
                },
            )
        }
    }

    override fun onSynonymRemoved(item: WordSynonymViewItem, cardId: Long) =
        editCard(cardId) {
            it.copy(
                synonyms = it.synonyms.filterIndexed { i, _ -> i != item.index },
            ).apply {
                updateCard(this, delay = 0)
            }
        }

    override fun onCardCreatePressed() {
        viewModelScope.launch {
            cardSetRepository.createCard()
        }
    }

    override fun onCardDeleted(cardId: Long) {
        findCard(cardId)?.let { card ->
            viewModelScope.launch {
                databaseCardWorker.deleteCard(card, timeSource.timeInMilliseconds())
            }
        }
    }

    override fun onPartOfSpeechChanged(newPartOfSpeech: WordTeacherWord.PartOfSpeech, cardId: Long) =
        editCard(cardId) {
            it.copy(
                partOfSpeech = newPartOfSpeech,
            ).apply {
                updateCard(this)
            }
        }

    private fun updateCard(card: Card, delay: Long = UPDATE_DELAY) {
        databaseCardWorker.updateCardCancellable(card, delay, timeSource.timeInMilliseconds())
    }

    override fun onBackPressed() {
        router?.closeCardSet()
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.article_error)
    }

    override fun getPlaceholder(viewItem: BaseViewItem<*>): StringDesc? {
        return when (val v = viewItem) {
            is WordTitleViewItem -> if (state.value.isRemoteCardSet) {
                StringDesc.Resource(MR.strings.card_title_placeholder_readonly)
            } else {
                StringDesc.Resource(MR.strings.card_title_placeholder)
            }
            is WordTranscriptionViewItem -> if (state.value.isRemoteCardSet) {
                StringDesc.Resource(MR.strings.card_transcription_placeholder_readonly)
            } else {
                StringDesc.Resource(MR.strings.card_transcription_placeholder)
            }
            else -> null
        }
    }

    private fun findCard(id: Long): Card? =
        inMemoryCardSet.value?.findCard(id) ?: cardSet.value.data()?.findCard(id)

    override fun onStartLearningClicked() {
        viewModelScope.launch {
            try {
                cardSet.value.data()?.let { set ->
                    val allCardIds = set.cards.filter {
                        it.progress.isReadyToLearn(timeSource)
                    }.map { it.id }
                    router?.openLearning(allCardIds)
                }
            } catch (e: Throwable) {
                // TODO: handle error
            }
        }
    }

    override fun onAddClicked() {
        viewModelScope.launch {
            cardSet.value.data()?.let {
                launch(Dispatchers.Default) {
                    val insertedCardSet = cardSetsRepository.insertCardSet(it)
                    cardSetRepository.loadAndObserveCardSet(insertedCardSet.id) {
                        state.update { CardSetVM.State.LocalCardSet(insertedCardSet.id) }
                    }
                }
            }
        }
    }

    enum class FocusLastType {
        Definition,
        Example,
        Synonym
    }

    sealed class PendingEvent(var isHandled: Boolean = false) {
        data class FocusLast(val type: FocusLastType, val cardId: Long): PendingEvent() {
            var prevEvent: FocusViewItemEvent? = null

            fun makeEvent(viewItem: BaseViewItem<*>): FocusViewItemEvent {
                prevEvent?.markAsHandled()
                val newEvent = object : FocusViewItemEvent(viewItem) {
                    override fun markAsHandled() {
                        super.markAsHandled()
                        this@FocusLast.isHandled = true
                    }
                }
                prevEvent = newEvent
                return newEvent
            }
        }
    }
}
