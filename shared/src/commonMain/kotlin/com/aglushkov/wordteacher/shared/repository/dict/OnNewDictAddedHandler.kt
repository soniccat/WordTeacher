package com.aglushkov.wordteacher.shared.repository.dict

import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.general.FileOpenController
import kotlinx.coroutines.runBlocking
import okio.Path

class OnNewDictAddedHandler(
    private val repository: DictRepository,
    private val analytics: Analytics,
): FileOpenController.SuccessHandler {
    override fun prepare(path: Path): Boolean {
        return true
    }

    override fun handle(path: Path): Boolean {
        analytics.send(AnalyticEvent.createActionEvent("FileOpenController.success.dict",
            mapOf("name" to path.name)))
        runBlocking { repository.importDicts() }
        return true
    }
}
