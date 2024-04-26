package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlin.properties.Delegates

// Represents 3 states of card state management
// SYNCING - pulling new state, merging and pushing a new state
// EDITING - we're editing cards on the editing screen or the learning screen
// UPDATING_SPANS - we're calculating spans to be able to hide terms from examples and synonyms while learning
// UPDATING_FREQUENCY - we're calculating word frequency to learn more popular words first
//
// Card updating queries shouldn't intersect to avoid data loss. Therefore, we need to be sure that they arent'
// executed in parallel. So, this class provides a state switching interface
class DatabaseCardWorker(
    private val databaseWorker: DatabaseWorker,
    private val spanUpdateWorker: SpanUpdateWorker,
    private val cardSetSyncWorker: CardSetSyncWorker,
    private val cardFrequencyUpdateWorker: CardFrequencyUpdateWorker,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val serialQueue = SerialQueue(Dispatchers.Main)
    private val editOperationCount = MutableStateFlow(0)
    private var stateStack by Delegates.observable(listOf<State>()) { _, _, new ->
        currentStateFlow.value = currentState
        Logger.v("state: $new", TAG)
    }

    val currentStateFlow = MutableStateFlow(State.NONE)
    val currentState: State
        get() = stateStack.lastOrNull() ?: State.NONE
    val previousState: State
        get() {
            if (stateStack.size <= 1) return State.NONE
            return stateStack[stateStack.size - 2]
        }

    init {
        // handle requests from cardSetSyncWorker
        scope.launch {
            cardSetSyncWorker.stateFlow.collect {
                val isPaused = it is CardSetSyncWorker.State.Paused
                when (it.innerState()) {
                    is CardSetSyncWorker.State.PullRequired,
                    is CardSetSyncWorker.State.PushRequired -> {
                        if (isPaused && currentState != State.SYNCING) {
                            if (currentState != State.EDITING) {
                                pushState(State.SYNCING)
                            } else if (!stateStack.contains(State.SYNCING)) {
                                stateStack = stateStack.subList(
                                    0,
                                    stateStack.size - 1
                                ) + State.SYNCING + currentState
                            }
                        }
                    }
                    is CardSetSyncWorker.State.Idle -> {
                        cardSetSyncWorker.pause()
                        popState(State.SYNCING)
                    }
                    else -> {}
                }
            }
        }

        // handle requests from spanUpdateWorker
        scope.launch {
            spanUpdateWorker.stateFlow.collect {
                if (it is com.aglushkov.wordteacher.shared.workers.State.Paused && it.hasWorkToDo) {
                    val cs = currentState
                    if (cs != State.EDITING && cs != State.SYNCING) {
                        pushState(State.UPDATING_SPANS)
                    } else if (!stateStack.contains(State.UPDATING_SPANS)) {
                        stateStack = listOf(State.UPDATING_SPANS, *stateStack.toTypedArray())
                    }
                } else if (it.isDone()) {
                    popState(State.UPDATING_SPANS)
                }
            }
        }

        // handle requests from cardFrequencyUpdateWorker
        scope.launch {
            cardFrequencyUpdateWorker.stateFlow.collect {
                if (it is com.aglushkov.wordteacher.shared.workers.State.Paused && it.hasWorkToDo) {
                    val cs = currentState
                    if (cs != State.EDITING && cs != State.SYNCING) {
                        pushState(State.UPDATING_FREQUENCY)
                    } else if (!stateStack.contains(State.UPDATING_FREQUENCY)) {
                        stateStack = listOf(State.UPDATING_FREQUENCY, *stateStack.toTypedArray())
                    }
                } else if (it.isDone()) {
                    popState(State.UPDATING_FREQUENCY)
                }
            }
        }

        // initial work queue
        serialQueue.send {
            val initialStack = listOf(/*State.UPDATING_FREQUENCY, State.UPDATING_SPANS,*/ State.SYNCING)
            prepareToNewState(initialStack.last(), State.NONE)
            stateStack = stateStack + initialStack
        }
    }

    fun untilFirstEditingFlow() = flow {
        if (currentState != State.EDITING) {
            currentStateFlow.takeWhile { it != State.EDITING }.collect {
                emit(it)
            }
        }

        emit(State.EDITING)
    }

    fun startEditing(): Clearable {
        if (currentState == State.EDITING) {
            return object : Clearable {
                override fun onCleared() {}
            }
        }

        pushState(State.EDITING)
        return object : Clearable {
            override fun onCleared() {
                endEditing()
            }
        }
    }

    fun endEditing() = popState(State.EDITING)

    suspend fun updateSpansAndStartEditing() {
        pushState(State.UPDATING_SPANS)
        waitUntilUpdatingSpansIsDone()
        popState(State.UPDATING_SPANS)
        pushStateAndWait(State.EDITING)
    }

    private fun pushState(newState: State) = serialQueue.send {
        pushStateInternal(newState)
    }

    suspend fun pushStateAndWait(newState: State) = serialQueue.sendAndWait {
        pushStateInternal(newState)
    }

    private suspend fun pushStateInternal(newState: State) {
        if (currentState == newState) {
            return
        }

        prepareToNewState(newState, currentState)
        stateStack = stateStack + newState
    }

    private fun popState(state: State) = serialQueue.send {
        popStateInternal(state)
    }

    suspend fun popStateAndWait(state: State) = serialQueue.sendAndWait {
        popStateInternal(state)
    }

    private suspend fun popStateInternal(state: State) {
        if (!stateStack.contains(state)) {
            Logger.e("Trying to pop not existing state ${state} when the current is ${currentState}", TAG)
            return
        }

        if (currentState == state && stateStack.isNotEmpty()) {
            prepareToNewState(previousState, currentState)
            stateStack = stateStack.take(stateStack.size - 1)
        } else {
            Logger.e("Trying to pop a wrong state ${state} when the current is ${currentState}", TAG)
        }
    }

    private suspend fun prepareToNewState(newState: State, fromState: State) {
        if (newState == fromState) {
            Logger.e("Trying to prepare for the same state ${newState}", TAG)
            return
        }

        when (fromState) {
            State.UPDATING_FREQUENCY -> {
                Logger.v("cardFrequencyUpdateWorker.pauseAndWaitUntilPausedOrDone", TAG)
                cardFrequencyUpdateWorker.pauseAndWaitUntilPausedOrDone()
            }
            State.UPDATING_SPANS -> {
                Logger.v("spanUpdateWorker.pauseAndWaitUntilPausedOrDone", TAG)
                spanUpdateWorker.pauseAndWaitUntilPausedOrDone()
            }
            State.EDITING -> {
                Logger.v("waitUntilEditingIsDone", TAG)
                waitUntilEditingIsDone()
            }
            State.SYNCING -> {
                Logger.v("cardSetSyncWorker.pauseAndWaitUntilDone", TAG)
                cardSetSyncWorker.pauseAndWaitUntilDone()
            }
            State.NONE -> {}
        }

        when(newState) {
            State.UPDATING_FREQUENCY -> {
                Logger.v("cardFrequencyUpdateWorker.resume()", TAG)
                cardFrequencyUpdateWorker.resume()
            }
            State.UPDATING_SPANS -> {
                Logger.v("spanUpdateWorker.resume()", TAG)
                spanUpdateWorker.resume()
            }
            State.SYNCING -> {
                Logger.v("cardSetSyncWorker.resume()", TAG)
                cardSetSyncWorker.resume()
            }
            State.EDITING, State.NONE -> {}
        }
    }

    suspend fun waitUntilUpdatingSpansIsDone() {
        spanUpdateWorker.waitUntilDone()
    }

    suspend fun waitUntilEditingIsDone() {
        editOperationCount.takeWhile { it > 0 }.collect()
    }

    suspend fun waitUntilSyncIsDone() {
        cardSetSyncWorker.waitUntilDone()
    }

    // Editing State Operation

    fun deleteCard(card: Card, modificationDate: Long) = serialQueue.send {
        deleteCardInternal(card, modificationDate)
    }

    suspend fun deleteCardAndWait(card: Card, modificationDate: Long) = serialQueue.sendAndWait {
        deleteCardInternal(card, modificationDate)
    }

    private suspend fun deleteCardInternal(card: Card, modificationDate: Long) = performEditOperation {
        databaseWorker.run {
            it.cards.removeCard(card.id, modificationDate)
        }
    }

//    suspend fun updateCard(card: Card) = serialQueue.send {
//        updateCardInternal(card)
//    }

    suspend fun updateCardAndWait(card: Card, modificationDate: Long?) = serialQueue.sendAndWait {
        performEditOperation {
            databaseWorker.run {
                it.cards.updateCard(card, modificationDate)
            }
        }
    }

    suspend fun updateCardSetInfo(cardSet: CardSet) = serialQueue.sendAndWait {
        performEditOperation {
            databaseWorker.runCancellable(cardSet.creationId) {
                it.cardSets.updateCardSetInfo(cardSet)
            }
        }
    }

//    private suspend fun updateCardInternal(card: Card) = performEditOperation {
//        databaseWorker.run {
//            database.cards.updateCard(card)
//        }
//    }

    fun updateCardCancellable(card: Card, delay: Long, modificationDate: Long?) = serialQueue.send {
        updateCardCancellableInternal(card, delay, modificationDate)
    }

    private suspend fun updateCardCancellableInternal(
        card: Card,
        delay: Long,
        modificationDate: Long?
    ) = performEditOperation {
        databaseWorker.runCancellable(
            id = "updateCard_" + card.id.toString(),
            delay
        ) {
            it.cards.updateCard(card, modificationDate)
        }
    }

    private suspend fun <T> performEditOperation(block: suspend () -> T): T {
        return try {
            validateEditingState()
            editOperationCount.update {
                val v = it + 1
                Logger.v("editOperationCount: $v", TAG)
                v
            }
            block()
        } finally {
            editOperationCount.update {
                val v = it - 1
                Logger.v("editOperationCount: $v", TAG)
                v
            }
        }
    }

    private fun validateEditingState() {
        if (currentState != State.EDITING) {
            Logger.e("Editing operation was called when state is ${currentState.name}", TAG)
        }
        pushState(State.EDITING)
    }

    enum class State {
        NONE,
        SYNCING,
        EDITING,
        UPDATING_SPANS,
        UPDATING_FREQUENCY,
    }
}

private val TAG = "DatabaseCardWorker"