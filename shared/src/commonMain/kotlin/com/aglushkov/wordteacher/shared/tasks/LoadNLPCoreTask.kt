package com.aglushkov.wordteacher.shared.tasks

import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import kotlinx.coroutines.channels.Channel

class LoadNLPCoreTask(
    private val nlpCore: NLPCore,
    override val childTasks: List<Task> = emptyList()
): Task {
    override suspend fun run(nextTasksChannel: Channel<Task>) {
        nlpCore.load()
        childTasks.onEach(nextTasksChannel::trySend)
    }
}