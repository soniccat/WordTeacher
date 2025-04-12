package com.aglushkov.wordteacher.shared.repository.dashboard

import com.aglushkov.wordteacher.shared.repository.history.HistoryRepository
import okio.FileSystem
import okio.Path

class ReadHeadlineRepository(
    historyPath: Path,
    fileSystem: FileSystem,
): HistoryRepository(historyPath, fileSystem, 50) {
}
