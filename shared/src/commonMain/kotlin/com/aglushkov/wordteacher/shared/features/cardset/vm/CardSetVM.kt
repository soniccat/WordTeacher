package com.aglushkov.wordteacher.shared.features.cardset.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
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

    fun onCardSetCreatePressed()
    fun onBackPressed()
    fun onTryAgainClicked()
    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?

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
            result += WordTitleViewItem(card.term, providers = emptyList())
            result += WordTranscriptionViewItem(card.transcription.orEmpty())
            result += WordPartOfSpeechViewItem(card.partOfSpeech.toStringDesc())

            card.definitions.onEach { def ->
                result += WordDefinitionViewItem(indentDefinitionString(def))
            }

            // Examples
            result += WordSubHeaderViewItem(
                StringDesc.Resource(MR.strings.word_section_examples),
                Indent.SMALL
            )

            card.examples.onEach { example ->
                result += WordExampleViewItem(example, Indent.SMALL)
            }

            // Synonyms
            result += WordSubHeaderViewItem(
                StringDesc.Resource(MR.strings.word_section_synonyms),
                Indent.SMALL
            )

            card.synonyms.onEach { synonym ->
                result += WordSynonymViewItem(synonym, Indent.SMALL)
            }

            result += WordDividerViewItem()
        }

        result += CreateCardViewItem()

        generateIds(result)
        return result
    }

    private fun generateIds(items: MutableList<BaseViewItem<*>>) {
        generateViewItemIds(items, viewItems.value.data().orEmpty(), idGenerator)
    }

    override fun onTryAgainClicked() {
        // TODO: do sth with articlesRepository
    }

    override fun onCardSetCreatePressed() {
        viewModelScope.launch {
            repository.createCard()
        }
    }

    override fun onBackPressed() {
        router.closeCardSet()
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.article_error)
    }
}
