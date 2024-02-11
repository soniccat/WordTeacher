package com.aglushkov.wordteacher.shared.repository.dict

import com.aglushkov.wordteacher.shared.general.FileOpenController
import kotlinx.coroutines.runBlocking
import okio.Path

class OnNewDictAddedHandler(
    private val repository: DictRepository,
): FileOpenController.SuccessHandler {
    override fun prepare(path: Path): Boolean {
        return true
    }

    override fun handle(path: Path): Boolean {
        runBlocking { repository.importDicts() }
        return true
    }
}
