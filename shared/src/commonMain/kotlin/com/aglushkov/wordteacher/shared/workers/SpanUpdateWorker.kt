package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.CardSpan
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.repository.cardset.findTermSpans
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SpanUpdateWorker (
    private val database: AppDatabase, // TODO: get rid of it
    private val databaseWorker: DatabaseWorker,
    private val nlpCore: NLPCore,
    private val nlpSentenceProcessor: NLPSentenceProcessor,
    private val timeSource: TimeSource
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val state = MutableStateFlow<State>(State.Paused(State.Done))
    val stateFlow: Flow<State> = state

    init {
        scope.launch {
            nlpCore.waitUntilInitialized()
            val nlpCoreCopy = nlpCore.clone()
            database.cards.selectCardsWithOutdatedSpans().asFlow().collect { query ->
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

                    Logger.v("wait until not paused", "SpanUpdateWorker")
                    state.takeWhile { it.isPaused() }.collect()
                    state.update {
                        if (it.isPendingPause()) {
                            it
                        } else {
                            State.InProgress
                        }
                    }

                    Logger.v("is in progress or pending pause (${cards.size}, ${state.value})", "SpanUpdateWorker")


                    cards.forEach { card ->
                        if (state.tryPauseIfPendingPause()) {
                            Logger.v("paused", "SpanUpdateWorker")
                            return@forEach
                        }

                        var defSpans = emptyList<List<CardSpan>>()
                        if (card.needToUpdateDefinitionSpans) {
                            defSpans = card.definitions.map {
                                if (state.tryPauseIfPendingPause()) {
                                    Logger.v("paused", "SpanUpdateWorker")
                                    return@forEach
                                }
                                findTermSpans(it, card.term, nlpCoreCopy, nlpSentenceProcessor)
                            }
                        }

                        var exampleSpans = emptyList<List<CardSpan>>()
                        if (card.needToUpdateExampleSpans) {
                            exampleSpans = card.examples.map {
                                if (state.tryPauseIfPendingPause()) {
                                    Logger.v("paused", "SpanUpdateWorker")
                                    return@forEach
                                }
                                findTermSpans(it, card.term, nlpCoreCopy, nlpSentenceProcessor)
                            }
                        }

                        if (state.tryPauseIfPendingPause()) {
                            Logger.v("paused", "SpanUpdateWorker")
                            return@forEach
                        }

                        databaseWorker.run {
                            database.cards.updateCard(
                                card = card.copy(
                                    definitionTermSpans = defSpans,
                                    exampleTermSpans = exampleSpans,
                                    needToUpdateDefinitionSpans = false,
                                    needToUpdateExampleSpans = false
                                ),
                                modificationDate = timeSource.timeInMilliseconds()
                            )
                        }
                    }

                    Logger.v("completed, paused=${ state.value.isPaused() }", "SpanUpdateWorker")
                } while (state.value.isPaused()) // we were interrupted, need to try again

                state.update { State.Done }
                Logger.v("Done", "SpanUpdateWorker")
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

sealed interface State {
    object Done: State // no requests to execute
    object InProgress: State // working with cards right now
    data class PendingPause(val pendingPrevState: State): State // pause request is in progress
    data class Paused(val pausedPrevState: State, val hasWorkToDo: Boolean = false): State // interrupted working with cards, will proceed after resuming
    //data class ResumeRequired(val pausedPrevState: State): State // like paused but we know that there's work to do and want to proceed

    fun toPaused(hasWorkToDo: Boolean = false) = when(this) {
        is Paused -> this.copy(hasWorkToDo = hasWorkToDo)
        else -> Paused(this.getPrevState())
    }

    fun resume() = when(this) {
        is Paused -> pausedPrevState
        is PendingPause -> pendingPrevState
        else -> this
    }

    fun isPendingPause() = when(this) {
        is PendingPause -> true
        else -> false
    }

    fun isPaused() = when(this) {
        is Paused -> true
        else -> false
    }

    fun isDone() = when(this) {
        is Done -> true
        else -> false
    }

    fun isPausedOrDone() = isPaused() || isDone()

    fun getPrevState(): State = when(this) {
        is PendingPause -> pendingPrevState
        is Paused -> pausedPrevState
        else -> this
    }
}

fun MutableStateFlow<State>.tryPauseIfPendingPause(): Boolean =
    updateAndGet {
        if (it.isPendingPause()) {
            it.toPaused()
        } else {
            it
        }
    } is State.Paused
