package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.takeWhile
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SerialQueue(
    dispatcher: CoroutineDispatcher
) { // execute suspended tasks one by one like iOS serial queue
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val channel = Channel<TaskItem<*>>(UNLIMITED)

    private val delayedTasks = HashMap<String, DelayedTaskData<*>>()

    init {
        scope.launch {
            channel.receiveAsFlow().collect {
                try {
                    it.task()
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Throwable) {
                    Logger.e(ex.message.orEmpty(), "SequentialWorker")
                }
            }
            Logger.e("omg SerialQueue has finished...")
        }
    }

    fun <T> send(task: suspend () -> T) {
        channel.trySend(TaskItem(task)) // always succeed as the channel is UNLIMITED
    }

    suspend fun <T> sendAndWait(task: suspend () -> T) =
        suspendCoroutine {
            send {
                val result = task()
                it.resume(result as T)
            }
        }

    // sending tasks with the same id replaces the task and increase task's time
    // TODO: rework cancellation logic in more native way, probably somehow with suspendCancellableCoroutine
    fun <T> sendWithDelay(id: String, time: Long, onCancelled: (() -> Unit)? = null, task: suspend () -> T) {
        (delayedTasks[id] as? DelayedTaskData<T>)?.delay(time, task, onCancelled) ?: run {
            val newTask = DelayedTaskData<T>(scope).delay(time, task, onCancelled)
            delayedTasks[id] = newTask
            channel.trySend(
                TaskItem {
                    newTask.runWhenReady()
                    delayedTasks.remove(id)
                }
            ) // always succeed as the channel is UNLIMITED
        }
    }

    suspend fun <T> sendWithDelayAndWait(id: String, time: Long, onCancelled: (() -> Unit)? = null, task: suspend () -> T) =
        suspendCoroutine {
            sendWithDelay(id, time, onCancelled) {
                val result = task()
                it.resume(result as T)
            }
        }

    private data class TaskItem<T>(
        val task: suspend () -> T
    )

    private class DelayedTaskData<T>(
        private val scope: CoroutineScope
    ) {
        private val readyState: MutableStateFlow<Boolean> = MutableStateFlow(false)
        private var turnStateJob: Job? = null
        private var task: (suspend () -> T)? = null
        private var cancellationHandler: (() -> Unit)? = null

        fun delay(
            time: Long,
            aTask: suspend () -> T,
            onCancelled: (() -> Unit)?,
        ): DelayedTaskData<T> {
            cancellationHandler?.invoke()
            task = null
            turnStateJob?.cancel()

            task = aTask
            cancellationHandler = onCancelled
            turnStateJob = scope.launch {
                delay(time)
                readyState.value = true
            }

            return this
        }

        suspend fun runWhenReady(): T {
            readyState.takeWhile { !it }.collect()
            return task!!.invoke()
        }
    }
}
