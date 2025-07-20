package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.Dispatchers

class DatabaseWorker(
    val database: AppDatabase,
) {
    private val serialQueue = SerialQueue(Dispatchers.IO)

    fun <T> read(task: (database: AppDatabase) -> T) {
        task(database)
    }

    fun <T> launch(task: (database: AppDatabase) -> T) {
        serialQueue.send {
            task(database)
        }
    }

    suspend fun <T> run(task: suspend (database: AppDatabase) -> T): T {
        return serialQueue.sendAndWait {
            task(database)
        }
    }

    suspend fun <T> runCancellable(
        id: String,
        delay: Long = UPDATE_DELAY,
        onCancelled: (() -> Unit)? = null,
        task: suspend (database: AppDatabase) -> T
    ): T {
        return serialQueue.sendWithDelayAndWait(id, delay, onCancelled) {
            task(database)
        }
    }

    fun launchCancellable(
        id: String,
        delay: Long = UPDATE_DELAY,
        task: (database: AppDatabase) -> Unit
    ) {
        serialQueue.sendWithDelay(id, delay) {
            task(database)
        }
    }
}

const val UPDATE_DELAY = 400L
