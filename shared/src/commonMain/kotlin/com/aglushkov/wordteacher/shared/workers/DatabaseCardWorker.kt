package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlin.properties.Delegates

// Represents 3 states of card state management
// SYNCING - pulling new state, merging and pushing a new state
// EDITING - we're editing cards on the editing screen or the learning screen
// UPDATING_SPANS - we're calculating spans
//
// Card updating queries shouldn't intersect to avoid data loss. Therefore, we need to be sure that they arent'
// executed in parallel. So, this class provides a state switching interface
class DatabaseCardWorker(
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val spanUpdateWorker: SpanUpdateWorker,
    private val cardSetSyncWorker: CardSetSyncWorker,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val serialQueue = SerialQueue()
    private val editOperationCount = MutableStateFlow(0)
    private var stateStack by Delegates.observable(listOf<State>()) { _, _, new ->
        currentStateFlow.value = currentState
        Logger.v("state: ${new}", "DatabaseCardWorker")
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
                when (it) {
                    is CardSetSyncWorker.State.PullRequired,
                    is CardSetSyncWorker.State.PushRequired -> {
                        //if (it.prevState is CardSetSyncWorker.State.PullRequired || it.prevState is CardSetSyncWorker.State.PushRequired) {
                            if (currentState != State.EDITING) {
                                pushState(State.SYNCING)
                            } else {
                                stateStack = stateStack.subList(0, stateStack.size - 1) + State.SYNCING + currentState
                            }
                       // }
                    }
                    is CardSetSyncWorker.State.Idle -> {
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
                    } else {
                        stateStack = listOf(State.UPDATING_SPANS, *stateStack.toTypedArray())
                    }
                } else if (it.isDone()) {
                    popState(State.UPDATING_SPANS)
                }
            }
        }

        // initial work queue
        serialQueue.send {
            val initialStack = listOf(State.UPDATING_SPANS, State.SYNCING)
            prepareToNewState(initialStack.last(), State.NONE)
            stateStack = stateStack + listOf(State.UPDATING_SPANS, State.SYNCING)
        }
    }

    fun untilFirstEditingFlow() = flow {
        currentStateFlow.takeWhile { it != State.EDITING }.collect {
            emit(it)
        }
        emit(State.EDITING)
    }

    fun startEditing() = pushState(State.EDITING)

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
        if (currentState == state && stateStack.isNotEmpty()) {
            prepareToNewState(previousState, currentState)
            stateStack = stateStack.take(stateStack.size - 1)
        } else {
            Logger.e("Trying to pop a wrong state ${state} when the current is ${currentState}", "DatabaseCardWorker")
        }
    }

    private suspend fun prepareToNewState(newState: State, fromState: State) {
        if (newState == fromState) {
            Logger.e("Trying to prepare for the same state ${newState}", "DatabaseCardWorker")
            return
        }

        when (fromState) {
            State.UPDATING_SPANS -> {
                Logger.v("spanUpdateWorker.pauseAndWaitUntilPausedOrDone", "DatabaseCardWorker")
                spanUpdateWorker.pauseAndWaitUntilPausedOrDone()
            }
            State.EDITING -> {
                Logger.v("waitUntilEditingIsDone", "DatabaseCardWorker")
                waitUntilEditingIsDone()
            }
            State.SYNCING -> {
                Logger.v("cardSetSyncWorker.pauseAndWaitUntilDone", "DatabaseCardWorker")
                cardSetSyncWorker.pauseAndWaitUntilDone()
            }
            State.NONE -> {}
        }

        when(newState) {
            State.UPDATING_SPANS -> {
                Logger.v("spanUpdateWorker.resume()", "DatabaseCardWorker")
                spanUpdateWorker.resume()
            }
            State.SYNCING -> {
                Logger.v("cardSetSyncWorker.resume()", "DatabaseCardWorker")
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

    fun deleteCard(card: Card) = serialQueue.send {
        deleteCardInternal(card)
    }

    suspend fun deleteCardAndWait(card: Card) = serialQueue.sendAndWait {
        deleteCardInternal(card)
    }

    private suspend fun deleteCardInternal(card: Card) = performEditOperation {
        databaseWorker.run {
            database.cards.removeCard(card.id)
        }
    }

    suspend fun updateCard(card: Card) = serialQueue.send {
        updateCardInternal(card)
    }

    suspend fun updateCardAndWait(card: Card) = serialQueue.sendAndWait {
        performEditOperation {
            databaseWorker.run {
                database.cards.updateCard(card)
            }
        }
    }

    private suspend fun updateCardInternal(card: Card) = performEditOperation {
        databaseWorker.run {
            database.cards.updateCard(card)
        }
    }

    fun updateCardCancellable(card: Card, delay: Long) = serialQueue.send {
        updateCardCancellableInternal(card, delay)
    }

    private suspend fun updateCardCancellableInternal(
        card: Card,
        delay: Long
    ) = performEditOperation {
        databaseWorker.runCancellable(
            id = "updateCard_" + card.id.toString(),
            runnable = {
                database.cards.updateCard(card)
            },
            delay
        )
    }

    private suspend fun <T> performEditOperation(block: suspend () -> T): T {
        return try {
            validateEditingState()
            editOperationCount.update {
                val v = it + 1
                Logger.v("editOperationCount: $v", "DatabaseCardWorker")
                v
            }
            block()
        } finally {
            editOperationCount.update {
                val v = it - 1
                Logger.v("editOperationCount: $v", "DatabaseCardWorker")
                v
            }
        }
    }

    private fun validateEditingState() {
        if (currentState != State.EDITING) {
            Logger.e("Editing operation was called when state is ${currentState.name}")
        }
        pushState(State.EDITING)
    }

    enum class State {
        NONE,
        SYNCING,
        EDITING,
        UPDATING_SPANS
    }
}

