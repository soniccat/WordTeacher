package com.aglushkov.wordteacher.shared.repository.cardset

import com.aglushkov.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.merge
import com.aglushkov.wordteacher.shared.general.resource.tryInResource
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.ImmutableCard
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
    private var updateCardJob: Job? = null // TODO: looks extremely error prone...

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

    suspend fun createCard(
        term: String = "",
        definitions: MutableList<String> = mutableListOf(),
        partOfSpeech: WordTeacherWord.PartOfSpeech = WordTeacherWord.PartOfSpeech.Undefined,
        transcription: String = "",
        synonyms: MutableList<String> = mutableListOf(),
        examples: MutableList<String> = mutableListOf()
    ): ImmutableCard? {
        val loadedCardSet = cardSet.value.data() ?: return null
        return scope.async(Dispatchers.Default) {
            database.cards.insertCard(
                setId = loadedCardSet.id,
                date = timeSource.getTimeInMilliseconds(),
                term = term,
                definitions = definitions,
                partOfSpeech = partOfSpeech,
                transcription = transcription,
                synonyms = synonyms,
                examples = examples
            )
        }.await()
    }

    suspend fun deleteCard(card: Card) {
        scope.async(Dispatchers.Default) {
            database.cards.removeCard(card.id)
        }.await()
    }

    suspend fun updateCard(card: Card, delay: Long = UPDATE_DELAY) {
        updateCardJob?.cancel()
        updateCardJob = scope.launch(Dispatchers.Default) {
            delay(delay)
            database.cards.updateCard(card)
        }
    }
}

const val UPDATE_DELAY = 200L