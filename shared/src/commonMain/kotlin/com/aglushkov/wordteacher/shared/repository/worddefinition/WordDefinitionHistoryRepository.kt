package com.aglushkov.wordteacher.shared.repository.worddefinition

import com.aglushkov.wordteacher.shared.repository.history.HistoryRepository
import okio.FileSystem
import okio.Path

class WordDefinitionHistoryRepository(
    historyPath: Path,
    fileSystem: FileSystem,
): HistoryRepository(historyPath, fileSystem, 300) {
}
