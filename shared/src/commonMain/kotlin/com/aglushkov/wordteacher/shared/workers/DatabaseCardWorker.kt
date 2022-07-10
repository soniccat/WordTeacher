package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.repository.db.AppDatabase

// Represents 2 states of card state management
// 1 - we're editing cards on the editing screen or the learning screen
// 2 - we're updating spans
// These states shouldn't intersect to avoid data loss.
class DatabaseCardWorker(
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val spanUpdateWorker: SpanUpdateWorker
) {
    private var state: State = State.UPDATING_SPANS

    suspend fun changeState() {

    }

    private enum class State {
        EDITING,
        UPDATING_SPANS
    }
}