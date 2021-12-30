package com.aglushkov.wordteacher.shared.features.cardset.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.MutableCard
import com.aglushkov.wordteacher.shared.model.MutableCardSet
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

interface CardSetVM {
    val state: State
    val cardSet: StateFlow<Resource<out CardSet>>
    val viewItems: StateFlow<Resource<List<BaseViewItem<*>>>>
    val eventFlow: Flow<Event>

    fun onCardCreatePressed()
    fun onCardDeleted(cardId: Long)
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
    final override val cardSet: StateFlow<Resource<out CardSet>> = repository.cardSet.map { cardSet ->
        mutableCardSet?.let { aMutableCardSet ->
            when(cardSet) {
                is Resource.Loaded -> cardSet.copyWith(mergeCardSets(cardSet.data, aMutableCardSet))
                else -> cardSet
            }
        } ?: run {
            cardSet
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    private var mutableCardSet: MutableCardSet? = null
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
        return dbCardSet.toImmutableCardSet().let { cardSet ->
            cardSet.copy(
                cards = cardSet.cards.map {
                    inMemoryCardSet.findCard(it.id) ?: it
                }
            )
        }
    }

    // TODO: consider storing only CardViewItem and separate UI parts in UI layer to leverage
    // all the Android Compose profit of diffing UI changes instead of what's going on in generateIds
    // (kinda hacky diffing with manual id management)
    private fun makeViewItems(loadedCardSet: CardSet): List<BaseViewItem<*>>  {
        val result = mutableListOf<BaseViewItem<*>>()

        loadedCardSet.cards.onEach { card ->
            val mutableCard = card.toMutableCard()

            val cardViewItems = mutableListOf<BaseViewItem<*>>()
            cardViewItems += WordTitleViewItem(card.term, providers = emptyList())
            cardViewItems += WordTranscriptionViewItem(card.transcription.orEmpty())
            cardViewItems += WordPartOfSpeechViewItem(card.partOfSpeech.toStringDesc())

            card.definitions.onEachIndexed { index, def ->
                cardViewItems += WordDefinitionViewItem(def, index = index, isLast = index == card.definitions.size - 1)
            }

            // Examples
            cardViewItems += WordSubHeaderViewItem(
                StringDesc.Resource(MR.strings.word_section_examples),
                Indent.SMALL
            )

            card.examples.onEachIndexed { index, example ->
                cardViewItems += WordExampleViewItem(example, Indent.SMALL, index, isLast = index == card.examples.size - 1)
            }

            // Synonyms
            cardViewItems += WordSubHeaderViewItem(
                StringDesc.Resource(MR.strings.word_section_synonyms),
                Indent.SMALL
            )

            card.synonyms.onEachIndexed { index, synonym ->
                Logger.v("Card synonym $index ($synonym)")
                cardViewItems += WordSynonymViewItem(synonym, Indent.SMALL, index, isLast = index == card.synonyms.size - 1)
            }

            result += CardViewItem(
                card = mutableCard,
                innerViewItems = cardViewItems
            )

            result += WordDividerViewItem()
        }

        result += CreateCardViewItem()

        generateIds(result)
        return result
    }

    private fun generateIds(items: MutableList<BaseViewItem<*>>) {
        generateViewItemIds(items, viewItems.value.data().orEmpty(), idGenerator) { newItem, oldItem ->
            if (newItem is CardViewItem && (oldItem is CardViewItem?)) {
                if (newItem.innerViewItems.size != oldItem?.innerViewItems?.size) {
                    // set ids depending on the item content to handle adding/deleting right
                    generateViewItemIds(
                        newItem.innerViewItems,
                        oldItem?.innerViewItems.orEmpty(),
                        idGenerator
                    )
                } else {
                    // keep ids not to alter them after content changing
                    newItem.innerViewItems.onEachIndexed { index, baseViewItem ->
                        baseViewItem.id = oldItem.innerViewItems[index].id
                    }
                }
            }
        }
    }

    override fun onTryAgainClicked() {
        // TODO: do sth with articlesRepository
    }

    override fun onItemTextChanged(text: String, item: BaseViewItem<*>, cardId: Long) {
        val cardSet = cardSet.value.data() ?: return
        val card = findCard(cardId) ?: return
        val mutableCard = obtainMutableCard(card, cardSet)

        withMutableCard(cardId) {
            var wasChanged = false

            when (item) {
                is WordTitleViewItem -> if (card.term != text) {
                    mutableCard.term = text
                    wasChanged = true
                }
                is WordTranscriptionViewItem -> if (card.transcription != text) {
                    mutableCard.transcription = text
                    wasChanged = true
                }
                is WordDefinitionViewItem -> card.definitions.getOrNull(item.index)?.let {
                    if (it != text) {
                        mutableCard.definitions[item.index] = text
                        wasChanged = true
                    }
                }
                is WordExampleViewItem -> card.examples.getOrNull(item.index)?.let {
                    if (it != text) {
                        mutableCard.examples[item.index] = text
                        wasChanged = true
                    }
                }
                is WordSynonymViewItem -> card.synonyms.getOrNull(item.index)?.let {
                    if (it != text) {
                        mutableCard.synonyms[item.index] = text
                        wasChanged = true
                    }
                }
            }

            if (wasChanged) {
                updateCard(mutableCard)
            }
        }
    }

    private fun obtainMutableCardSet(cardSet: CardSet) =
        mutableCardSet ?: cardSet.toMutableCardSet().apply {
                cards = emptyList()
                mutableCardSet = this
            }

    private fun withMutableCard(cardId: Long, block: (card: MutableCard) -> Unit) {
        val cardSet = cardSet.value.data() ?: return
        val card = findCard(cardId) ?: return
        block(obtainMutableCard(card, cardSet))
    }

    private fun obtainMutableCard(card: Card, cardSet: CardSet): MutableCard {
        val mutableCardSet = obtainMutableCardSet(cardSet)
        return mutableCardSet.findCard(card.id) ?: card.toMutableCard().apply {
            mutableCardSet.addCard(this)
        }
    }

    override fun onAddDefinitionPressed(cardId: Long) =
        withMutableCard(cardId) {
            it.definitions += ""
            updateCard(it, delay = 0)
        }

    override fun onDefinitionRemoved(item: WordDefinitionViewItem, cardId: Long) =
        withMutableCard(cardId) {
            it.definitions.removeAt(item.index)
            updateCard(it, delay = 0)
        }

    override fun onAddExamplePressed(cardId: Long) =
        withMutableCard(cardId) {
            it.examples += ""
            updateCard(it, delay = 0)
        }

    override fun onExampleRemoved(item: WordExampleViewItem, cardId: Long) {
        withMutableCard(cardId) {
            it.examples.removeAt(item.index)
            updateCard(it, delay = 0)
        }
    }

    override fun onAddSynonymPressed(cardId: Long) =
        withMutableCard(cardId) {
            it.synonyms += ""
            updateCard(it, delay = 0)
        }

    override fun onSynonymRemoved(item: WordSynonymViewItem, cardId: Long) =
        withMutableCard(cardId) {
            it.synonyms.removeAt(item.index)
            updateCard(it, delay = 0)
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

    private fun updateCard(card: Card, delay: Long = UPDATE_DELAY) {
        viewModelScope.launch {
            try {
                repository.updateCard(card, delay)
                mutableCardSet?.removeCard(card)
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
        cardSet.value.data()?.findCard(id)
}
