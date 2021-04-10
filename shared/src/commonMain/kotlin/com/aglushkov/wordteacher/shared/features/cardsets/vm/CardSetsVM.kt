package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.Time
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.ShortArticle
import com.aglushkov.wordteacher.shared.model.ShortCardSet
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CardSetsVM(
    cardSetsRepository: CardSetsRepository,
    private val time: Time,
    private val router: CardSetsRouter
): ViewModel() {

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

    fun onCreateTextCardSetClicked() {
        router.openAddCardSet()
    }

    fun onCardSetClicked(item: CardSetViewItem) {
        router.openCardSet(item.id)
    }

    private fun buildViewItems(cardSets: List<ShortCardSet>): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()
        cardSets.forEach {
            items.add(CardSetViewItem(it.id, it.name, time.stringDate(it.date)))
        }

        return items
    }
}