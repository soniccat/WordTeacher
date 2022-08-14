package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.flow.*
import kotlin.properties.Delegates

// Represents 2 states of card state management
// 1 - we're editing cards on the editing screen or the learning screen
// 2 - we're updating spans
// Card updating queries shouldn't intersect to avoid data loss. Therefore, we need to be sure that they arent'
// executed in parallel. So, this class provides a state switching interface
class DatabaseCardWorker(
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val spanUpdateWorker: SpanUpdateWorker
) {
    //private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val serialQueue = SerialQueue()
    private val editOperationCount = MutableStateFlow(0)
    private var stateStack by Delegates.observable(listOf(DEFAULT_STATE)) { _, _, new ->
        currentStateFlow.value = currentState
        Logger.v("state: ${new}", "DatabaseCardWorker")
    }

    val currentStateFlow = MutableStateFlow(DEFAULT_STATE)
    val currentState: State
        get() = stateStack.lastOrNull() ?: DEFAULT_STATE
    val previousState: State
        get() {
            if (stateStack.size <= 1) return DEFAULT_STATE
            return stateStack[stateStack.size - 2]
        }

    fun untilFirstEditingFlow() = flow {
        currentStateFlow.takeWhile { it != State.EDITING }.collect {
            emit(it)
        }
        emit(State.EDITING)
    }

    fun pushState(newState: State) = serialQueue.send {
        pushStateInternal(newState)
    }

    suspend fun pushStateAndWait(newState: State) = serialQueue.sendAndWait {
        pushStateInternal(newState)
    }

    private suspend fun pushStateInternal(newState: State) {
        if (currentState == newState) {
            return
        }

        prepareToNewState(newState)
        stateStack = stateStack + newState
    }

    fun popState(state: State) = serialQueue.send {
        popStateInternal(state)
    }

    suspend fun popStateAndWait(state: State) = serialQueue.sendAndWait {
        popStateInternal(state)
    }

    private suspend fun popStateInternal(state: State) {
        if (currentState == state && stateStack.isNotEmpty()) {
            prepareToNewState(previousState)
            stateStack = stateStack.take(stateStack.size - 1)
        }
    }

    private suspend fun prepareToNewState(newState: State) {
        when (newState) {
            State.EDITING -> {
                Logger.v("pauseAndWaitUntilPausedOrDone", "DatabaseCardWorker")
                spanUpdateWorker.pauseAndWaitUntilPausedOrDone()
            }
            State.UPDATING_SPANS -> {
                Logger.v("waitUntilEditingIsDone", "DatabaseCardWorker")
                waitUntilEditingIsDone()
                Logger.v("spanUpdateWorker.resume()", "DatabaseCardWorker")
                spanUpdateWorker.resume()
            }
        }
    }

//    suspend fun waitUntilUpdatingSpansIsStarted() {
//        spanUpdateWorker.waitUntilStarted()
//    }

    suspend fun waitUntilUpdatingSpansIsDone() {
        spanUpdateWorker.waitUntilDone()
    }

    suspend fun waitUntilEditingIsDone() {
        editOperationCount.takeWhile { it > 0 }.collect()
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

//    suspend fun updateCardSafely(card: Card) = sequentialWorker.sendAndWait {
//        performEditOperation {
//            try {
//                updateCard(card)
//            } catch (e: CancellationException) {
//                throw e
//            } catch (e: Throwable) {
//                // TODO: handle error
//                Logger.e("DatabaseCardWorker.updateCardSafely", e.toString())
//                throw e
//            }
//        }
//    }

    fun updateCardCancellable(card: Card, delay: Long) = serialQueue.send {
        updateCardCancellableInternal(card, delay)
    }

    suspend fun updateCardCancellableAndWait(card: Card, delay: Long) = serialQueue.sendAndWait {
        updateCardCancellableInternal(card, delay)
    }

//    suspend fun updateCardCancellableSafely(card: Card, delay: Long): Card {
//        return try {
//            updateCardCancellable(card, delay)
//        } catch (e: CancellationException) {
//            throw e
//        } catch (e: Throwable) {
//            // TODO: handle error
//            Logger.e("DatabaseCardWorker.updateCardSafely", e.toString())
//            throw e
//        }
//    }

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
        //        databaseWorker.run {
        //            database.cards.updateCard(card)
        //        }
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
        EDITING,
        UPDATING_SPANS
    }
}

private val DEFAULT_STATE = DatabaseCardWorker.State.UPDATING_SPANS