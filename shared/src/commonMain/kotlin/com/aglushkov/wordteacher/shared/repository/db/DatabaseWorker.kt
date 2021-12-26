package com.aglushkov.wordteacher.shared.repository.db

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: import TaskManager with a custom DB pool
class DatabaseWorker {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // In one moment only cancellableJobMap or jobSet might be non empty
    // if one is non empty then we fill the pending of another
    // We switch queues only when one becomes empty

    private val cancellableJobMap = mutableMapOf<Long, Job>()
    private val pendingCancellableRunnable = mutableMapOf<Long, CancellableRunnable>()

    private val jobSet = mutableSetOf<Job>()
    private val pendingRunnable = mutableListOf<Runnable>()

    suspend fun runCancellable(id: Long, runnable: Runnable, delay: Long = UPDATE_DELAY) {
        runCancellableInternal(id, runnable, delay, true)
    }

    private suspend fun runCancellableInternal(id: Long, runnable: Runnable, delay: Long = UPDATE_DELAY, shouldRunNext: Boolean) {
        if (jobSet.isNotEmpty() || !cancellableJobMap.containsKey(id) && cancellableJobMap.isNotEmpty()) {
            pendingCancellableRunnable[id] = CancellableRunnable(runnable, delay)
            return
        }

        cancellableJobMap[id]?.cancelAndJoin()
        if (cancellableJobMap.containsKey(id)) {
            Logger.e("DatabaseWorker.runCancellable", "expect empty cancellableJobMap")
        }

        val job = scope.launch(Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            Logger.e("DatabaseWorker.runCancellable", throwable.toString())
        }) {
            delay(delay)
            runnable.run()

//            try {
//                delay(delay)
//                runnable.run()
//            } catch (ex: CancellationException) {
//                throw ex
//            } catch (ex: Throwable) {
//                Logger.e("DatabaseWorker.runCancellable", ex.toString())
//            }
        }

        cancellableJobMap[id] = job
        job.join()

//        try {
//            job.join()
//        } catch (ex: CancellationException) {
//            throw ex
//        }

        cancellableJobMap.remove(id)

        if (shouldRunNext) {
            launchPendingIfNeeded()
        }
    }

    suspend fun run(runnable: Runnable) {
        runInternal(runnable, shouldRunNext = true)
    }

    private suspend fun runInternal(runnable: Runnable, shouldRunNext: Boolean) {
        if (cancellableJobMap.isNotEmpty() || jobSet.isNotEmpty()) {
            pendingRunnable.add(runnable)
            return
        }

        val job = scope.launch(Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            Logger.e("DatabaseWorker.run", throwable.toString())
        }) {
            runnable.run()
//            try {
//                runnable.run()
//            } catch (ex: CancellationException) {
//                throw ex
//            } catch (ex: Throwable) {
//                Logger.e("DatabaseWorker.run", ex.toString())
//            }
        }

        jobSet.add(job)
        job.join()
//        try {
//            job.join()
//        } catch (ex: CancellationException) {
//            throw ex
//        }
        jobSet.remove(job)

        if (shouldRunNext) {
            launchPendingIfNeeded()
        }
    }

    private suspend fun launchPendingIfNeeded() {
        if (jobSet.isNotEmpty() || cancellableJobMap.isNotEmpty()) {
            // wait until they both becomes empty
            return
        }

        // now we expect to have only one pending queue filled
        if (pendingRunnable.isNotEmpty() && pendingCancellableRunnable.isNotEmpty()) {
            Logger.e("DatabaseWorker", "We've got two filled worker queues (runnables: ${pendingRunnable.size} cancellable runnables: ${pendingCancellableRunnable.size})")
        }

        // going to drain pending queue including newly added runnables
        if (pendingRunnable.isNotEmpty()) {
            while (pendingRunnable.isNotEmpty()) {
                val runnableToRun = pendingRunnable.toList()
                runnableToRun.onEach {
                    runInternal(it, shouldRunNext = false)
                }
                pendingRunnable.removeAll(runnableToRun)
            }

        } else if (pendingCancellableRunnable.isNotEmpty()) {
            while (pendingCancellableRunnable.isNotEmpty()) {
                val runnableToRun = pendingCancellableRunnable.toMap()
                runnableToRun.onEach { entry ->
                    runCancellableInternal(entry.key, entry.value.runnable, entry.value.delay, shouldRunNext = false)
                }
                runnableToRun.onEach {
                    pendingCancellableRunnable.remove(it.key)
                }
            }
        }

        launchPendingIfNeeded()
    }
}

private data class CancellableRunnable(
    val runnable: Runnable,
    val delay: Long,
)

const val UPDATE_DELAY = 200L
