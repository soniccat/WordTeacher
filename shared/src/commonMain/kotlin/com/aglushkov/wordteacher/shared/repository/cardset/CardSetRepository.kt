package com.aglushkov.wordteacher.shared.repository.cardset

import com.aglushkov.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.merge
import com.aglushkov.wordteacher.shared.general.resource.tryInResource
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CardSetRepository(
    private val database: AppDatabase
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<CardSet>>(Resource.Uninitialized())

    val cardSet: StateFlow<Resource<CardSet>> = stateFlow
    private var loadJob: Job? = null

    suspend fun loadCardSet(id: Long) {
        loadJob?.cancel()
        loadJob = scope.launch(Dispatchers.Default) {
            combine(
                flow = database.cardSets.selectCardSet(id).asFlow().map {
                    tryInResource { it.executeAsOne() }
                },
                flow2 = database.cards.selectCards(id).asFlow().map {
                    tryInResource { it.executeAsList() }
                },
                transform = { cardSetRes, cardsRes ->
                    cardSetRes.merge(cardsRes) { cardSet, cards ->
                        if (cardSet != null && cards != null) {
                            cardSet.cards = cards.orEmpty()
                            cardSet
                        } else {
                            cardSet
                        }
                    }
                }
            ).collect {
                stateFlow.value = it
            }
        }
    }
}
