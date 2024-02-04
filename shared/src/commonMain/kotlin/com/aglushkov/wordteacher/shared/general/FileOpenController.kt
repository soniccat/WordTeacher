package com.aglushkov.wordteacher.shared.general

import com.aglushkov.wordteacher.shared.general.resource.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.Path

interface FileOpenController {

    interface Validator {
        fun validateFile(path: Path): Boolean
    }

    interface SuccessHandler {
        fun prepare(path: Path): Boolean
        fun handle(path: Path): Boolean
    }

    val state: StateFlow<Resource<Unit>>

    suspend fun chooseFile(): Resource<Unit>
}

class FileOpenCompositeSuccessHandler(
    private val handlers: List<FileOpenController.SuccessHandler>
): FileOpenController.SuccessHandler {

    override fun prepare(path: Path): Boolean {
        for(h in handlers) {
            if (!h.prepare(path)) {
                return false
            }
        }

        return true
    }

    override fun handle(path: Path): Boolean {
        for(h in handlers) {
            if (!h.handle(path)) {
                return false
            }
        }

        return true
    }
}
