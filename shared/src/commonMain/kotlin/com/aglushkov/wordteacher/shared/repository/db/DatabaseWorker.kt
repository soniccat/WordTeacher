package com.aglushkov.wordteacher.shared.repository.db

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine

// TODO: write tests
// TODO: import TaskManager with a custom DB pool
class DatabaseWorker {
    val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // In one moment only cancellableJobMap or jobSet might be non empty
    // if one is non empty then we fill the pending of another
    // We switch queues only when one becomes empty

    private val cancellableDeferredMap = mutableMapOf<String, Deferred<*>>()
    private val pendingCancellableRunnable = mutableMapOf<String, CancellableRunnable<*>>()

    private val deferredSet = mutableSetOf<Deferred<*>>()
    private val pendingRunnable = mutableListOf<WorkerRunnableInternal<*>>()

    suspend fun <T> runCancellable(id: String, runnable: WorkerRunnable<T>, delay: Long = UPDATE_DELAY): T {
        return runCancellableInternal(id, WorkerRunnableInternal(runnable), delay, true)
    }

    private suspend fun <T> runCancellableInternal(
        id: String,
        runnable: WorkerRunnableInternal<T>,
        delay: Long = UPDATE_DELAY,
        shouldRunNext: Boolean
    ) : T = supervisorScope {
        if (deferredSet.isNotEmpty() || !cancellableDeferredMap.containsKey(id) && cancellableDeferredMap.isNotEmpty()) {
            pendingCancellableRunnable[id]?.let {
                it.runnable.completion(null, RuntimeException("Cancelled"))
            }

            pendingCancellableRunnable[id] = CancellableRunnable(runnable, delay)
            return@supervisorScope suspendCancellableCoroutine <T> {
                runnable.completion = { v, e ->
                    if (v != null) {
                        it.resumeWith(Result.success(v))
                    } else {
                        it.resumeWith(Result.failure(e ?: RuntimeException("Unknown result")))
                    }
                }
            }
        }

        cancellableDeferredMap[id]?.cancelAndJoin()

        val deferred = scope.async(Dispatchers.Default) {
            delay(delay)
            runnable.work.invoke()
        }

        cancellableDeferredMap[id] = deferred
        try {
            val result = deferred.await()
            runnable.completion.invoke(result, null)

            return@supervisorScope result
        } catch (e: Throwable) {
            runnable.completion.invoke(null, e)
            throw e
        } finally {
            if (cancellableDeferredMap[id] == deferred) {
                cancellableDeferredMap.remove(id)
            }

            if (shouldRunNext) {
                scope.launch {
                    launchPendingIfNeeded()
                }
            }
        }
    }

    suspend fun <T> run(runnable: WorkerRunnable<T>) : T {
        return runInternal(WorkerRunnableInternal(runnable), shouldRunNext = true)
    }

    private suspend fun <T> runInternal(
        runnable: WorkerRunnableInternal<T>,
        shouldRunNext: Boolean
    ): T = supervisorScope {
        if (cancellableDeferredMap.isNotEmpty() || deferredSet.isNotEmpty()) {
            pendingRunnable.add(runnable)
            return@supervisorScope suspendCancellableCoroutine<T> {
                runnable.completion = { v, e ->
                    if (v != null) {
                        it.resumeWith(Result.success(v))
                    } else {
                        it.resumeWith(Result.failure(e ?: RuntimeException("Unknown result")))
                    }
                }
            }
        }

        val deferred = scope.async(Dispatchers.Default) {
            runnable.work.invoke()
        }

        deferredSet.add(deferred)

        try {
            val result = deferred.await()
            runnable.completion.invoke(result, null)

            return@supervisorScope result
        } catch (e: Throwable) {
            runnable.completion.invoke(null, e)
            throw e
        } finally {
            deferredSet.remove(deferred)

            if (shouldRunNext) {
                scope.launch {
                    launchPendingIfNeeded()
                }
            }
        }
    }

    private suspend fun launchPendingIfNeeded() {
        if (deferredSet.isNotEmpty() || cancellableDeferredMap.isNotEmpty()) {
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

        if (pendingRunnable.isNotEmpty() || pendingCancellableRunnable.isNotEmpty()) {
            launchPendingIfNeeded()
        }
    }
}

private data class CancellableRunnable<T>(
    val runnable: WorkerRunnableInternal<T>,
    val delay: Long,
)

private data class WorkerRunnableInternal<T>(
    val work: () -> T,
    var completion: (value: T?, e: Throwable?) -> Unit = { _, _ -> }
)

typealias WorkerRunnable<T> = () -> T

const val UPDATE_DELAY = 200L
