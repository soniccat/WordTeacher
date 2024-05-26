package com.aglushkov.wordteacher.shared.repository.cardsetsearch

import com.aglushkov.wordteacher.shared.general.extensions.updateData
import com.aglushkov.wordteacher.shared.general.resource.*
import com.aglushkov.wordteacher.shared.general.toOkResponse
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.service.SpaceCardSetSearchService
import com.aglushkov.wordteacher.shared.service.SpaceCardSetService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class CardSetSearchRepository(
    private val service: SpaceCardSetSearchService,
    private val cardSetService: SpaceCardSetService,
) {
    data class SearchCardSet(
        val cardSet: CardSet,
        val fullCardSetRes: Resource<CardSet> = Resource.Uninitialized(),
    )

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val cardSets = MutableStateFlow<Resource<List<SearchCardSet>>>(Resource.Uninitialized())

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
                service.search(aQuery).toOkResponse().cardSets.orEmpty().map { SearchCardSet(it) }
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

    fun cardSetByRemoteId(remoteId: String): CardSet? {
        return cardSets.value.data().orEmpty().firstOrNull { it.cardSet.remoteId == remoteId }?.cardSet
    }

    fun removeCardSet(remoteId: String) {
        cardSets.update {
            it.updateData { carSets ->
                carSets?.filter { it.cardSet.remoteId != remoteId }
            }
        }
    }

    fun loadRemoteCardSet(remoteId: String): Flow<Resource<CardSet>> {
        val searchCardSet = cardSets.value.data().orEmpty().firstOrNull {
            it.cardSet.remoteId == remoteId
        } ?: return flowOf(Resource.Error(RuntimeException("Unknown card set id")))

        val fullCardSetRes = searchCardSet.fullCardSetRes
        if (fullCardSetRes.isLoadedOrLoading()) {
            return flowOf(fullCardSetRes)
        }

        return loadResource {
            cardSetService.getById(remoteId).toOkResponse().cardSet
        }.onEach { res ->
            cardSets.updateData {
                it.map {
                    if (it.cardSet.remoteId == remoteId) {
                        it.copy(fullCardSetRes = fullCardSetRes)
                    } else {
                        it
                    }
                }
            }
        }
    }
}
