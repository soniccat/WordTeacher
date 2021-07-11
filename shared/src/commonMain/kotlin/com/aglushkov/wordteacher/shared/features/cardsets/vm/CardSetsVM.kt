package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.ShortCardSet
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CardSetsVM(
    val cardSetsRepository: CardSetsRepository,
    private val timeSource: TimeSource,
    private val router: CardSetsRouter
): ViewModel() {

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    val eventFlow = eventChannel.receiveAsFlow()
    private val cardSetsFlow = cardSetsRepository.cardSets
    val cardSets = MutableStateFlow<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())

    init {
        viewModelScope.launch {
            cardSetsFlow.map {
                Logger.v("build view items")
                it.copyWith(buildViewItems(it.data() ?: emptyList()))
            }.forward(cardSets)
        }
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

    fun onCardSetClicked(item: CardSetViewItem) {
        router.openCardSet(item.id)
    }

    fun onStartLearningClicked() {
        // TODO: open learning sreen
//        router.openStartLearning
    }

    private fun buildViewItems(cardSets: List<ShortCardSet>): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()
        cardSets.forEach {
            items.add(CardSetViewItem(it.id, it.name, timeSource.stringDate(it.date)))
        }

        return items
    }
}

object ShowCreateSetEvent: Event
