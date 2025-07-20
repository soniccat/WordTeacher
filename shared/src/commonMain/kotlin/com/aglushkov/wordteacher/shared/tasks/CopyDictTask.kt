package com.aglushkov.wordteacher.shared.tasks

import com.aglushkov.wordteacher.shared.general.okio.writeToWithVersioning
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository

import kotlinx.coroutines.channels.Channel
import okio.FileSystem
import okio.Path
import okio.Source

class CopyDictTask(
    private val src: Source?,
    private val copyPath: Path,
    private val fileSystem: FileSystem,
    private val dictRepository: DictRepository,
    private val lastVersion: Int,
    private val settings: SettingStore,
): Task {
    override suspend fun run(nextTasksChannel: Channel<Task>) {
        if (
            fileSystem.writeToWithVersioning(
                src,
                copyPath,
                COPY_DICT_TASK_VERSION + copyPath.name,
                lastVersion,
                settings
            )
        ) {
            dictRepository.importDicts()
        }
    }
}

private const val COPY_DICT_TASK_VERSION = "copyDictTaskVersion_"