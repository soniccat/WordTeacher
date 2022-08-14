package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.v
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
    private val state = MutableStateFlow<State>(State.Done)
//    private val pendingState = MutableStateFlow(State.Idle)

    init {
        scope.launch {
            nlpCore.waitUntilInitialized()
            val nlpCoreCopy = nlpCore.clone()
            database.cards.selectCardsWithOutdatedSpans().asFlow().collect { query ->
//                var wasPaused = false
                do {
                    Logger.v("wait until not paused", "SpanUpdateWorker")
//                    wasPaused = false
                    state.takeWhile { it.isPaused() }.collect()
                    //pauseState.waitUntilFalse() // wait while we're on pause

                    val cards = query.executeAsList() // execute or re-execute the query
                    //inProgressState.value = true
                    state.update {
                        if (it.isPendingPause()) {
                            it
                        } else {
                            State.InProgress
                        }
                    }

                    Logger.v("is in progress or pending pause (${cards.size}, ${state.value})", "SpanUpdateWorker")

                    cards.forEach { card ->
                        if (state.value.isPendingPause()) {
//                            wasPaused = true;
                            state.update { it.toPaused() }
                            Logger.v("paused", "SpanUpdateWorker")
                            return@forEach
                        }

                        var defSpans = emptyList<List<Pair<Int, Int>>>()
                        if (card.needToUpdateDefinitionSpans) {
                            defSpans = card.definitions.map {
                                if (state.value.isPendingPause()) {
//                                    wasPaused = true
                                    state.update { it.toPaused() }
                                    Logger.v("paused", "SpanUpdateWorker")
                                    return@forEach
                                }
                                findTermSpans(it, card.term, nlpCoreCopy, nlpSentenceProcessor)
                            }
                        }

                        var exampleSpans = emptyList<List<Pair<Int, Int>>>()
                        if (card.needToUpdateExampleSpans) {
                            exampleSpans = card.examples.map {
                                if (state.value.isPendingPause()) {
//                                    wasPaused = true
                                    state.update { it.toPaused() }
                                    Logger.v("paused", "SpanUpdateWorker")
                                    return@forEach
                                }
                                findTermSpans(it, card.term, nlpCoreCopy, nlpSentenceProcessor)
                            }
                        }

                        if (state.value.isPendingPause()) {
//                            wasPaused = true
                            state.update { it.toPaused() }
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
                                )
                            )
                        }
                    }

//                    inProgressState.value = false
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
                State.PendingPause
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

private sealed interface State {
    object Done: State // no requests to execute
    object InProgress: State // working with cards right now
    object PendingPause: State // pause request is in progress
    data class Paused(val prevState: State): State // interrupted working with cards, will proceed after resuming

    fun toPaused() = when(this) {
        is Paused -> this
        else -> Paused(this)
    }

    fun resume() = when(this) {
        is Paused -> prevState
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
}
