package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SerialQueue { // like iOS serial queue
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val channel = Channel<TaskItem<*>>(UNLIMITED)

    init {
        scope.launch {
            channel.receiveAsFlow().collect {
                try {
                    val result = it.task.invoke()
                    it.completion?.invoke(result)
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Throwable) {
                    Logger.e(ex.message.orEmpty(), "SequentialWorker")
                }
            }
            Logger.e("omg rrrr")
        }
    }

    fun send(task: suspend () -> Unit) {
        channel.trySend(TaskItem(task)) // always succeed as the channel is UNLIMITED
    }

    suspend fun <T> sendAndWait(task: suspend () -> T) =
        suspendCoroutine<T> {
            channel.trySend(
                TaskItem(
                    task = task,
                    completion = { result ->
                        it.resume(result as T)
                    }
                )
            )
        }

    private data class TaskItem<T>(
        val task: suspend () -> T,
        val completion: ((result: Any?) -> Unit)? = null
    )
}
