package com.aglushkov.wordteacher.shared.tasks

import com.aglushkov.wordteacher.shared.general.okio.writeToWithLockFile
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import okio.FileSystem
import okio.Path
import okio.Source

class CopyDictTask(
    private val src: Source?,
    private val copyPath: Path,
    private val fileSystem: FileSystem,
    private val dictRepository: DictRepository,
): Task {
    override suspend fun run() {
        if (!fileSystem.exists(copyPath)) {
            if (src != null) {
                fileSystem.writeToWithLockFile(src, copyPath)
                dictRepository.importDicts()
            } else {
                return
            }
        }
    }
}
