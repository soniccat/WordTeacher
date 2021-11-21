package com.aglushkov.wordteacher.shared.features.cardset.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.toStringDesc
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface CardSetVM {
    val state: State
    val cardSet: StateFlow<Resource<CardSet>>
    val viewItems: StateFlow<Resource<List<BaseViewItem<*>>>>
    val eventFlow: Flow<Event>

    fun onCardCreatePressed()
    fun onCardDeleted(card: Card)
    fun onItemTextChanged(text: String, item: BaseViewItem<*>, card: Card)
    fun onAddDefinitionPressed(card: Card)
    fun onDefinitionRemoved(item: WordDefinitionViewItem, card: Card)
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
    override val cardSet: StateFlow<Resource<CardSet>> = repository.cardSet
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

    private fun buildViewItems(cardSetRes: Resource<CardSet>): List<BaseViewItem<*>> {
        return when (cardSetRes) {
            is Resource.Loaded -> {
                makeViewItems(cardSetRes)
            }
            else -> emptyList()
        }
    }

    private fun makeViewItems(loadedRes: Resource.Loaded<CardSet>): List<BaseViewItem<*>>  {
        val result = mutableListOf<BaseViewItem<*>>()

        loadedRes.data.cards.onEach { card ->
            val cardViewItems = mutableListOf<BaseViewItem<*>>()
            cardViewItems += WordTitleViewItem(card.term, providers = emptyList())
            cardViewItems += WordTranscriptionViewItem(card.transcription.orEmpty())
            cardViewItems += WordPartOfSpeechViewItem(card.partOfSpeech.toStringDesc())

            card.definitions.onEachIndexed { index, def ->
                cardViewItems += WordDefinitionViewItem(def, index)
            }

            // Examples
            cardViewItems += WordSubHeaderViewItem(
                StringDesc.Resource(MR.strings.word_section_examples),
                Indent.SMALL
            )

            card.examples.onEach { example ->
                cardViewItems += WordExampleViewItem(example, Indent.SMALL)
            }

            // Synonyms
            cardViewItems += WordSubHeaderViewItem(
                StringDesc.Resource(MR.strings.word_section_synonyms),
                Indent.SMALL
            )

            card.synonyms.onEach { synonym ->
                cardViewItems += WordSynonymViewItem(synonym, Indent.SMALL)
            }

            result += CardViewItem(
                card = card,
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

    override fun onItemTextChanged(text: String, item: BaseViewItem<*>, card: Card) {
        when (item) {
            is WordTitleViewItem -> card.term = text
            is WordTranscriptionViewItem -> card.transcription = text
            is WordDefinitionViewItem -> card.definitions[item.index] = text
        }

        updateCard(card)
    }

    override fun onAddDefinitionPressed(card: Card) {
        card.definitions += ""
        updateCard(card)
    }

    override fun onDefinitionRemoved(item: WordDefinitionViewItem, card: Card) {
        card.definitions.removeAt(item.index)
        updateCard(card)
    }

    override fun onCardCreatePressed() {
        viewModelScope.launch {
            repository.createCard()
        }
    }

    override fun onCardDeleted(card: Card) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }

    private fun updateCard(card: Card) {
        viewModelScope.launch {
            repository.updateCard(card)
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
}
