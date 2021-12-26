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
import com.aglushkov.wordteacher.shared.repository.db.DatabaseWorker
import com.aglushkov.wordteacher.shared.repository.db.UPDATE_DELAY
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CardSetRepository(
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val timeSource: TimeSource
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
                            cardSet.copy(cards = cards.orEmpty())
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
        return databaseWorker.run {
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
        }
    }

    suspend fun deleteCard(card: Card) {
        databaseWorker.run {
            database.cards.removeCard(card.id)
        }
    }

    suspend fun updateCard(card: Card, delay: Long) {
        databaseWorker.runCancellable(
            id = "updateCard_" + card.id.toString(),
            runnable = {
                database.cards.updateCard(card)
            },
            delay
        )
    }
}
