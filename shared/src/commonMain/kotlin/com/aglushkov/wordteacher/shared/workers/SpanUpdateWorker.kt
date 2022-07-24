package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilFalse
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.repository.cardset.findTermSpans
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SpanUpdateWorker (
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val nlpCore: NLPCore,
    private val nlpSentenceProcessor: NLPSentenceProcessor
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val pauseState = MutableStateFlow(false)
    private val inProgressState = MutableStateFlow(false)

    init {
        scope.launch {
            nlpCore.waitUntilInitialized()
            val nlpCoreCopy = nlpCore.clone()
            database.cards.selectCardsWithOutdatedSpans().asFlow().collect { query ->
                var wasPaused = false
                do {
                    wasPaused = false
                    pauseState.waitUntilFalse() // wait while we're on pause

                    val cards = query.executeAsList() // execute or re-execute the query
                    inProgressState.value = true

                    cards.forEach { card ->
                        if (pauseState.value) { wasPaused = true; return@forEach }

                        var defSpans = emptyList<List<Pair<Int, Int>>>()
                        if (card.needToUpdateDefinitionSpans) {
                            defSpans = card.definitions.map {
                                if (pauseState.value) { wasPaused = true; return@forEach }
                                findTermSpans(it, card.term, nlpCoreCopy, nlpSentenceProcessor)
                            }
                        }

                        var exampleSpans = emptyList<List<Pair<Int, Int>>>()
                        if (card.needToUpdateExampleSpans) {
                            exampleSpans = card.examples.map {
                                if (pauseState.value) { wasPaused = true; return@forEach }
                                findTermSpans(it, card.term, nlpCoreCopy, nlpSentenceProcessor)
                            }
                        }

                        if (pauseState.value) { wasPaused = true; return@forEach }

                        databaseWorker.run {
                            database.cards.updateCard(
                                card = card.copy(
                                    definitionTermSpans = defSpans,
                                    exampleTermSpans = exampleSpans,
                                    needToUpdateDefinitionSpans = false,
                                    needToUpdateExampleSpans = false
                                )
                            )
                        }
                    }

                    inProgressState.value = false
                } while (wasPaused) // we were interrupted, need to try again
            }
        }
    }

    fun pause() {
        pauseState.value = true
    }

    fun resume() {
        pauseState.value = false
    }

    suspend fun waitUntilDone() {
        inProgressState.waitUntilFalse()
    }

    suspend fun pauseAndWaitUntilDone() {
        pause()
        waitUntilDone()
    }
}
