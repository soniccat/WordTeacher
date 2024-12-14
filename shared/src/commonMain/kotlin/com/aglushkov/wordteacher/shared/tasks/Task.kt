package com.aglushkov.wordteacher.shared.tasks

import kotlinx.coroutines.channels.Channel

interface Task {
    val childTasks: List<Task>
        get() = emptyList()

    suspend fun run(nextTasksChannel: Channel<Task>)
}