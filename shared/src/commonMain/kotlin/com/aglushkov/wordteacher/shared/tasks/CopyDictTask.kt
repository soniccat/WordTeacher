package com.aglushkov.wordteacher.shared.tasks

import com.aglushkov.wordteacher.shared.general.okio.writeToWithVersioning
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.russhwolf.settings.coroutines.FlowSettings
import okio.FileSystem
import okio.Path
import okio.Source

class CopyDictTask(
    private val src: Source?,
    private val copyPath: Path,
    private val fileSystem: FileSystem,
    private val dictRepository: DictRepository,
    private val lastVersion: Int,
    private val flowSettings: FlowSettings,
): Task {
    override suspend fun run() {
        if (
            fileSystem.writeToWithVersioning(
                src,
                copyPath,
                COPY_DICT_TASK_VERSION + copyPath.name,
                lastVersion,
                flowSettings
            )
        ) {
            dictRepository.importDicts()
        }
    }
}

private const val COPY_DICT_TASK_VERSION = "copyDictTaskVersion_"