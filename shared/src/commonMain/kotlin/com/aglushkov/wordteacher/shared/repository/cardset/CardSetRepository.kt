package com.aglushkov.wordteacher.shared.repository.cardset

import com.aglushkov.wordteacher.db.DBCardSet
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import com.aglushkov.wordteacher.shared.general.resource.merge
import com.aglushkov.wordteacher.shared.general.resource.tryInResource
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CardSetRepository(
    private val databaseWorker: DatabaseWorker,
    private val timeSource: TimeSource
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<CardSet>>(Resource.Uninitialized())

    val cardSet: StateFlow<Resource<CardSet>> = stateFlow
    private var loadJob: Job? = null

    fun cardSetWithoutCardsFlow(id: Long): Flow<Resource<CardSet>> {
        return databaseWorker.database.cardSets.selectCardSetWithoutCards(id).asFlow().map {
            tryInResource(canTryAgain = true) { it.executeAsOne() }
        }
    }

    fun loadCardSetWithoutCards(id: Long): Flow<Resource<CardSet>> {
        return loadResource {
            databaseWorker.database.cardSets.selectCardSetWithoutCards(id).executeAsOne()
        }
    }

    suspend fun loadCardSet(id: Long) {
        loadJob?.cancel()
        loadJob = scope.launch(Dispatchers.Default) {
            combine(
                flow = databaseWorker.database.cardSets.selectCardSetWithoutCards(id).asFlow().map {
                    tryInResource(canTryAgain = true) { it.executeAsOne() }
                },
                flow2 = databaseWorker.database.cards.selectCards(id).asFlow().map {
                    tryInResource(canTryAgain = true) { it.executeAsList() }
                },
                transform = { cardSetRes, cardsRes ->
                    cardSetRes.merge(cardsRes) { cardSet, cards ->
                        if (cardSet != null && cards != null) {
                            cardSet.copy(cards = cards)
                        } else {
                            cardSet
                        }
                    }
                }
            ).collect(stateFlow)
        }
    }

    suspend fun createCard(
        term: String = "",
        definitions: MutableList<String> = mutableListOf(),
        partOfSpeech: WordTeacherWord.PartOfSpeech = WordTeacherWord.PartOfSpeech.Undefined,
        transcription: String = "",
        synonyms: MutableList<String> = mutableListOf(),
        examples: MutableList<String> = mutableListOf()
    ): Card {
        val loadedCardSet = cardSet.value.data()!!
        return databaseWorker.run {
            it.cards.insertCard(
                setId = loadedCardSet.id,
                creationDate = timeSource.timeInstant(),
                term = term,
                definitions = definitions,
                partOfSpeech = partOfSpeech,
                transcription = transcription,
                synonyms = synonyms,
                examples = examples
            )
        }
    }
}
