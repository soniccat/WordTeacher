package com.aglushkov.wordteacher.shared.tasks

interface Task {
    suspend fun run()
}