package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.ShortCardSet
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface CardSetsVM {
    val state: State
    val eventFlow: Flow<Event>
    val cardSets: StateFlow<Resource<List<BaseViewItem<*>>>>

    fun restore(newState: State)
    fun onStartLearningClicked()
    fun onCardSetClicked(item: CardSetViewItem)
    fun onCardSetRemoved(item: CardSetViewItem)
    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?
    fun onTryAgainClicked()

    @Parcelize
    class State: Parcelable {
    }
}

open class CardSetsVMImpl(
    override var state: CardSetsVM.State = CardSetsVM.State(),
    val cardSetsRepository: CardSetsRepository,
    private val router: CardSetsRouter,
    private val timeSource: TimeSource
): ViewModel(), CardSetsVM {

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()
    private val cardSetsFlow = cardSetsRepository.cardSets
    override val cardSets = MutableStateFlow<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())

    init {
        viewModelScope.launch {
            cardSetsFlow.map {
                Logger.v("build view items")
                it.copyWith(buildViewItems(it.data() ?: emptyList()))
            }.forward(cardSets)
        }
    }

    override fun restore(newState: CardSetsVM.State) {
        state = newState
    }

    override fun onCleared() {
        super.onCleared()
        eventChannel.cancel()
    }

    fun onCreateTextCardSetClicked() {
        eventChannel.offer(ShowCreateSetEvent)
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

    }

    override fun onStartLearningClicked() {
        router.openStartLearning()
    }

    private fun buildViewItems(cardSets: List<ShortCardSet>): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()
        cardSets.forEach {
            items.add(CardSetViewItem(it.id, it.name, timeSource.stringDate(it.date)))
        }

        return items
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.cardsets_error)
    }

    override fun onTryAgainClicked() {
        // TODO: do sth with articlesRepository
    }
}

object ShowCreateSetEvent: Event
