package com.aglushkov.wordteacher.shared.repository.cardsetsearch

import com.aglushkov.wordteacher.shared.general.resource.*
import com.aglushkov.wordteacher.shared.general.toOkResponse
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.service.SpaceCardSetSearchService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class CardSetSearchRepository(
    private val service: SpaceCardSetSearchService,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val cardSets = MutableStateFlow<Resource<List<CardSet>>>(Resource.Uninitialized())

    var query: String? = null
        private set
    private var searchJob: Job? = null

    fun search() {
        query?.let {
            search(it)
        }
    }

    fun search(aQuery: String) {
        if (query == aQuery && !cardSets.value.isLoadedOrLoading()) {
            return
        }

        val oldJob = searchJob

        query = aQuery
        searchJob = scope.launch(Dispatchers.Default) {
            oldJob?.cancelAndJoin()

            loadResource(initialValue = cardSets.value.toUninitialized().bumpVersion()) {
                service.search(aQuery).toOkResponse().cardSets.orEmpty()
            }.collect(cardSets)
        }
    }

    fun clear() {
        val oldJob = searchJob
        searchJob = null
        query = null

        scope.launch(Dispatchers.Default) {
            oldJob?.cancelAndJoin()
            cardSets.update { Resource.Uninitialized() }
        }
    }

    fun removeAtIndex(i: Int) {
        cardSets.update {
            it.updateData { carSets ->
                carSets?.filterIndexed { index, _ ->  index != i }
            }
        }
    }
}
