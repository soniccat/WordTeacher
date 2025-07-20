package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.extensions.splitByChunks
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CardFrequencyUpdateWorker (
    private val databaseWorker: DatabaseWorker,
    private val frequencyDatabase: WordFrequencyDatabase,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val state = MutableStateFlow<State>(State.Paused(State.Done))
    val stateFlow: Flow<State> = state

    init {
        scope.launch {
            frequencyDatabase.waitUntilInitialized()

            databaseWorker.database.cards.selectCardsWithUndefinedFrequency().asFlow().collect { query ->
                do {
                    val cards = query.executeAsList() // execute or re-execute the query
                    if (cards.isEmpty()) {
                        break
                    } else if (state.value.isPaused()) {
                        state.update {
                            if (it.isPaused()) {
                                it.toPaused(true)
                            } else {
                                it
                            }
                        }
                    }

                    Logger.v("wait until not paused", TAG)
                    state.takeWhile { it.isPaused() }.collect()
                    state.update {
                        if (it.isPendingPause()) {
                            it
                        } else {
                            State.InProgress
                        }
                    }

                    Logger.v("is in progress or pending pause (${cards.size}, ${state.value})", TAG)

                    cards.splitByChunks(20).forEach br@ { cardChunk ->
                        if (state.tryPauseIfPendingPause()) {
                            Logger.v("paused", TAG)
                            return@br
                        }

                        var frequencies = listOf<Double>()
                        try {
                            frequencies = frequencyDatabase.resolveFrequencyForWords(cardChunk.map { it.term })
                        } catch (e: Exception) {
                            Logger.e("updateCardFrequency exception " + e.message, TAG)
                        }
                        databaseWorker.run {
                            try {
                                cardChunk.onEachIndexed { cardIndex, card ->
                                    it.cards.updateCardFrequency(card.id, frequencies[cardIndex])
                                }
                            } catch (e: Exception) {
                                Logger.e("updateCardFrequency exception " + e.message, TAG)
                            }
                        }
                    }

                    Logger.v("completed, paused=${ state.value.isPaused() }", TAG)
                } while (state.value.isPaused()) // we were interrupted, need to try again

                state.update { State.Done }
                Logger.v("Done", TAG)
            }
        }
    }

    fun pause() {
        state.update {
            if (it.isPaused()) {
                it
            } else if (it.isDone()) {
                it.toPaused()
            } else {
                State.PendingPause(it)
            }
        }
    }

    fun resume() {
        state.update { it.resume() }
    }

    suspend fun waitUntilDone() {
        state.takeWhile { !it.isDone() }.collect()
    }

    suspend fun waitUntilPausedOrDone() {
        state.takeWhile { !it.isPausedOrDone() }.collect()
    }

    suspend fun pauseAndWaitUntilPausedOrDone() {
        pause()
        waitUntilPausedOrDone()
    }
}

private const val TAG = "CardFrequencyUpdateWorker"