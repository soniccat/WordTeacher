package com.aglushkov.wordteacher.shared.features.cardset.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.toStringDesc
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.repository.db.UPDATE_DELAY
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface CardSetVM: Clearable {
    val state: State
    val cardSet: StateFlow<Resource<out CardSet>>
    val viewItems: StateFlow<Resource<List<BaseViewItem<*>>>>
    val eventFlow: Flow<Event>

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
    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?
    fun getPlaceholder(viewItem: BaseViewItem<*>): StringDesc?

    fun onStartLearningClicked()

    @Parcelize
    data class State (
        val cardSetId: Long
    ): Parcelable
}

open class CardSetVMImpl(
    override var state: CardSetVM.State,
    private val router: CardSetRouter,
    private val repository: CardSetRepository,
    private val timeSource: TimeSource,
    private val idGenerator: IdGenerator
): ViewModel(), CardSetVM {

    // Contains cards being edited and haven't been synched with DB
    private var inMemoryCardSet = MutableStateFlow<CardSet?>(null)
    final override val cardSet: StateFlow<Resource<CardSet>> = combine(
        repository.cardSet, inMemoryCardSet,
        transform = { cardSet, inMemoryCardSet ->
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
    }).stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()

    override val viewItems = cardSet.map {
        Logger.v("build view items")
        it.copyWith(buildViewItems(it))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    fun restore(newState: CardSetVM.State) {
        state = newState

        viewModelScope.launch {
            repository.loadCardSet(state.cardSetId)
        }
    }

    private fun buildViewItems(cardSetRes: Resource<out CardSet>): List<BaseViewItem<*>> {
        return when (cardSetRes) {
            is Resource.Loaded -> {
                makeViewItems(cardSetRes.data)
            }
            else -> emptyList()
        }
    }

    private fun mergeCardSets(dbCardSet: CardSet, inMemoryCardSet: CardSet): CardSet {
        // replace db cards with in-memory cards
        Logger.v("merge cardSet $dbCardSet\n with $inMemoryCardSet")

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

    private fun makeViewItems(loadedCardSet: CardSet): List<BaseViewItem<*>>  {
        val result = mutableListOf<BaseViewItem<*>>()

        loadedCardSet.cards.onEach { card ->
            val cardViewItems = mutableListOf<BaseViewItem<*>>()
            cardViewItems += WordTitleViewItem(card.term, providers = emptyList(), cardId = card.id)
            cardViewItems += WordTranscriptionViewItem(card.transcription.orEmpty(), cardId = card.id)
            cardViewItems += WordPartOfSpeechViewItem(card.partOfSpeech.toStringDesc(), card.partOfSpeech, cardId = card.id)

            if (card.definitions.isEmpty()) {
                cardViewItems += WordDefinitionViewItem("", index = 0, isLast = true, cardId = card.id)
            } else {
                card.definitions.onEachIndexed { index, def ->
                    cardViewItems += WordDefinitionViewItem(def, index = index, isLast = index == card.definitions.size - 1, cardId = card.id)
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
                cardViewItems += WordExampleViewItem(example, Indent.SMALL, index, isLast = index == card.examples.size - 1, cardId = card.id)
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
                Logger.v("Card synonym $index ($synonym)")
                cardViewItems += WordSynonymViewItem(synonym, Indent.SMALL, index, isLast = index == card.synonyms.size - 1, cardId = card.id)
            }

            result += cardViewItems
            /*CardViewItem(
                cardId = card.id,
                innerViewItems = cardViewItems
            )*/

            result += WordDividerViewItem()
        }

        result += CreateCardViewItem()

        generateIds(result)
        return result
    }

    private fun generateIds(items: MutableList<BaseViewItem<*>>) {
        generateViewItemIds(items, viewItems.value.data().orEmpty(), idGenerator)
//        generateViewItemIds(items, viewItems.value.data().orEmpty(), idGenerator) { newItem, oldItem ->
//            if (newItem is CardViewItem && (oldItem is CardViewItem?)) {
//                if (newItem.innerViewItems.size != oldItem?.innerViewItems?.size) {
//                    // set ids depending on the item content to handle adding/deleting right
//                    generateViewItemIds(
//                        newItem.innerViewItems,
//                        oldItem?.innerViewItems.orEmpty(),
//                        idGenerator
//                    )
//                } else {
//                    // keep ids not to alter them after content changing
//                    newItem.innerViewItems.onEachIndexed { index, baseViewItem ->
//                        baseViewItem.id = oldItem.innerViewItems[index].id
//                    }
//                }
//            }
//        }
    }

    override fun onTryAgainClicked() {
        // TODO: do sth with articlesRepository
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
                        }
                    )
                is WordTranscriptionViewItem ->
                    card.copy(
                        transcription = if (card.transcription != text) {
                            text
                        } else {
                            card.transcription
                        }
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
                        }
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
                        }
                    )
                is WordSynonymViewItem ->
                    card.copy(
                        synonyms = card.synonyms.mapIndexed { index, s ->
                            if (item.index == index) {
                                text
                            } else {
                                s
                            }
                        }
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
                }
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

    override fun onAddDefinitionPressed(cardId: Long) =
        editCard(cardId) {
            it.copy(
                definitions = if (it.definitions.lastOrNull() != "") {
                    it.definitions + ""
                } else {
                    it.definitions
                }
            )
        }

    override fun onDefinitionRemoved(item: WordDefinitionViewItem, cardId: Long) =
        editCard(cardId) {
            it.copy(
                definitions = if (it.definitions.size <= 1) {
                    listOf("")
                } else {
                    it.definitions.filterIndexed { i, _ -> i != item.index }
                }
            ).apply {
                updateCard(this, delay = 0)
            }
        }

    override fun onAddExamplePressed(cardId: Long) =
        editCard(cardId) {
            it.copy(
                examples = if (it.examples.lastOrNull() != "") {
                    it.examples + ""
                } else {
                    it.examples
                }
            )
        }

    override fun onExampleRemoved(item: WordExampleViewItem, cardId: Long) =
        editCard(cardId) {
            it.copy(
                examples = it.examples.filterIndexed { i, _ -> i != item.index }
            ).apply {
                updateCard(this, delay = 0)
            }
        }

    override fun onAddSynonymPressed(cardId: Long) =
        editCard(cardId) {
            it.copy(
                synonyms = if (it.synonyms.lastOrNull() != "") {
                    it.synonyms + ""
                } else {
                    it.synonyms
                }
            )
        }

    override fun onSynonymRemoved(item: WordSynonymViewItem, cardId: Long) =
        editCard(cardId) {
            it.copy(
                synonyms = it.synonyms.filterIndexed { i, _ -> i != item.index }
            ).apply {
                updateCard(this, delay = 0)
            }
        }

    override fun onCardCreatePressed() {
        viewModelScope.launch {
            repository.createCard()
        }
    }

    override fun onCardDeleted(cardId: Long) {
        findCard(cardId)?.let { card ->
            viewModelScope.launch {
                repository.deleteCard(card)
            }
        }
    }

    override fun onPartOfSpeechChanged(newPartOfSpeech: WordTeacherWord.PartOfSpeech, cardId: Long) =
        editCard(cardId) {
            it.copy(
                partOfSpeech = newPartOfSpeech
            ).apply {
                updateCard(this)
            }
        }

    private fun updateCard(card: Card, delay: Long = UPDATE_DELAY) {
        viewModelScope.launch {
            try {
                repository.updateCard(card, delay)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // TODO: handle
            }
        }
    }

    override fun onBackPressed() {
        router.closeCardSet()
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.article_error)
    }

    override fun getPlaceholder(viewItem: BaseViewItem<*>): StringDesc? {
        return when (val v = viewItem) {
            is WordTitleViewItem -> StringDesc.Resource(MR.strings.card_title_placeholder)
            is WordTranscriptionViewItem -> StringDesc.Resource(MR.strings.card_transcription_placeholder)
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
                    router.openLearning(allCardIds)
                }
            } catch (e: Throwable) {
                // TODO: handle error
            }
        }
    }
}
