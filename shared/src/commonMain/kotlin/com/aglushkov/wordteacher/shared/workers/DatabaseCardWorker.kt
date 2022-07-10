package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.CancellationException
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
    private val editOperationCount = MutableStateFlow(0)
    private var stateStack by Delegates.observable(listOf(State.UPDATING_SPANS)) { _, _, new ->
        currentStateFlow.value = currentState
    }

    val currentStateFlow = MutableStateFlow(State.UPDATING_SPANS)
    val currentState: State
        get() = stateStack.lastOrNull() ?: State.UPDATING_SPANS

    fun untilFirstEditingFlow() = flow {
        currentStateFlow.takeWhile { it != State.EDITING }.collect {
            emit(it)
        }
        emit(State.EDITING)
    }

    suspend fun pushState(newState: State) {
        if (currentState == newState) {
            return
        }

        when (newState) {
            State.EDITING -> {
                spanUpdateWorker.pauseAndWaitUntilDone()
            }
            State.UPDATING_SPANS -> {
                waitUntilEditingIsDone()
                spanUpdateWorker.resume()
            }
        }

        stateStack = stateStack + newState
    }

    fun popState(state: State) {
        if (currentState == state && stateStack.isNotEmpty()) {
            stateStack = stateStack.takeLast(1)
        }
    }

    suspend fun waitUntilEditingIsDone() {
        editOperationCount.takeWhile { it >= 0 }.collect()
    }

    // Editing State Operation

    suspend fun deleteCard(card: Card) = performEditOperation {
        databaseWorker.run {
            database.cards.removeCard(card.id)
        }
    }

    suspend fun updateCard(card: Card) = performEditOperation {
        databaseWorker.run {
            database.cards.updateCard(card)
        }
    }

    suspend fun updateCardSafely(card: Card) = performEditOperation {
        try {
            updateCard(card)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // TODO: handle error
            Logger.e("DatabaseCardWorker.updateCardSafely", e.toString())
            throw e
        }
    }

    suspend fun updateCardCancellable(card: Card, delay: Long) = performEditOperation {
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

    suspend fun updateCardCancellableSafely(card: Card, delay: Long): Card {
        return try {
            updateCardCancellable(card, delay)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // TODO: handle error
            Logger.e("DatabaseCardWorker.updateCardSafely", e.toString())
            throw e
        }
    }

    suspend fun <T> performEditOperation(block: suspend () -> T): T {
        return try {
            validateEditingState()
            editOperationCount.update { it + 1 }
            block()
        } finally {
            editOperationCount.update { it - 1 }
        }
    }

    private suspend fun validateEditingState() {
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