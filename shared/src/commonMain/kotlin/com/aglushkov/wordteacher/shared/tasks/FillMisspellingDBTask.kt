package com.aglushkov.wordteacher.shared.tasks

import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect

abstract class FillMisspellingDBTask(
    private val settings: SettingStore,
    private val lastVersion: Int,
): Task {
    override suspend fun run(nextTasksChannel: Channel<Task>) {
        val currentVersion = settings.int(MISSPELLING_FILLED_DB_VERSION_KEY, -1)
        val isFilled = currentVersion == lastVersion
        if (isFilled) {
            return
        }

        process()

        childTasks.onEach(nextTasksChannel::trySend)
    }

    abstract suspend fun process()

    fun markAsComplete() {
        settings[MISSPELLING_FILLED_DB_VERSION_KEY] = lastVersion
    }

}

private const val MISSPELLING_FILLED_DB_VERSION_KEY = "MISSPELLING_FILLED_DB_VERSION_KEY"