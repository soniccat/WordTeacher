package com.aglushkov.wordteacher.shared.repository.cardset

import com.aglushkov.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.merge
import com.aglushkov.wordteacher.shared.general.resource.tryInResource
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CardSetRepository(
    private val database: AppDatabase,
    private val timeSource: TimeSource
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<CardSet>>(Resource.Uninitialized())

    val cardSet: StateFlow<Resource<CardSet>> = stateFlow
    private var loadJob: Job? = null
    private var updateCardJob: Job? = null

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

    suspend fun createCard(): Card? {
        val loadedCardSet = cardSet.value.data() ?: return null
        return scope.async(Dispatchers.Default) {
            val newCard = Card(
                id = -1,
                date = timeSource.getTimeInMilliseconds(),
                term = "",
                definitions = emptyList(),
                partOfSpeech = WordTeacherWord.PartOfSpeech.Undefined,
                transcription = "",
                synonyms = emptyList(),
                examples = emptyList()
            )

            database.cards.insertCard(loadedCardSet.id, newCard)
            newCard.id = database.cards.insertedCardId()!!
            newCard
        }.await()
    }

    suspend fun updateCard(card: Card) {
        updateCardJob?.cancel()
        updateCardJob = scope.launch(Dispatchers.Default) {
            delay(200)
            database.cards.updateCard(card)
        }
    }
}
