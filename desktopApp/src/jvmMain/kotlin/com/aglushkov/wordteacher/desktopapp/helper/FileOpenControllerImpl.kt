package com.aglushkov.wordteacher.desktopapp.helper

import com.aglushkov.wordteacher.shared.general.FileOpenController
import com.aglushkov.wordteacher.shared.general.okio.useAsTmp
import com.aglushkov.wordteacher.shared.general.okio.writeTo
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
import okio.sink
import okio.source
import java.awt.FileDialog
import java.awt.Frame

class FileOpenControllerImpl(
    private val name: String,
    private val mimeTypes: List<String>,
    private val tmpPath: Path,
    private val dstPath: Path,
    private val validator: FileOpenController.Validator,
    private val successHandler: FileOpenController.SuccessHandler,
): FileOpenController {
    var parent: Frame? = null
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    override val state: MutableStateFlow<Resource<Unit>> = MutableStateFlow(Resource.Uninitialized())

    override suspend fun chooseFile(): Resource<Unit> {
        state.update { Resource.Loading() }
        val fd = FileDialog(parent, "Choose a file", FileDialog.LOAD)
        fd.directory = "./"
        fd.file = "*.db"
        fd.isVisible = true
        fd.setFilenameFilter { file, s -> mimeTypes.any { file.endsWith(it) } }
        fd.file?.let { choseFile ->
            val choseFilePath = fd.directory.toPath().div(choseFile)
            withContext(Dispatchers.Default) {
                loadResource {
                    tmpPath.useAsTmp {
                        choseFilePath.toFile().source().writeTo(it.toFile().sink())
                        validator.validateFile(it)
                        it.toFile().source().writeTo(dstPath.toFile().sink())
                        successHandler.handle(dstPath)
                    }
                    Unit
                }.collect(state)
            }
            Unit
        } ?: run {
            state.update { it.toUninitialized() }
        }

        return state.value
    }
}
