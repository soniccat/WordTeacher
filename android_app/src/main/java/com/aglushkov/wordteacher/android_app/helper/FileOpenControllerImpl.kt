package com.aglushkov.wordteacher.android_app.helper

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.aglushkov.wordteacher.shared.general.FileOpenController
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilLoadedOrError
import com.aglushkov.wordteacher.shared.general.okio.deleteIfExists
import com.aglushkov.wordteacher.shared.general.okio.useAsTmp
import com.aglushkov.wordteacher.shared.general.okio.writeTo
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.tryInResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.sink
import okio.source

class FileOpenControllerImpl(
    private val name: String,
    private val mimeTypes: List<String>,
    private val tmpPath: Path,
    private val dstPath: Path,
    private val validator: FileOpenController.Validator,
    private val successHandler: FileOpenController.SuccessHandler,
): FileOpenController {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var openDocumentLauncher: ActivityResultLauncher<Array<String>>? = null
    private val requestResource: MutableStateFlow<Resource<Unit>> = MutableStateFlow(Resource.Uninitialized())

//    @SuppressLint("Recycle")
    fun bind(activity: ComponentActivity) {
        openDocumentLauncher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { result ->
            mainScope.launch(Dispatchers.Default) {
                requestResource.update {
                    tryInResource {
                        if (result == null) {
                            throw NullPointerException("OpenDocument result is null")
                        }
                        tmpPath.useAsTmp {
                            activity.contentResolver.openInputStream(result)?.source()
                                ?.writeTo(it.toFile().sink())
                            validator.validateFile(it)

                            it.toFile().source()?.writeTo(dstPath.toFile().sink())
                            successHandler.handle(dstPath)
                        }
                    }
                }
            }
        }
    }

    override suspend fun chooseFile(): Resource<Unit> {
        requestResource.update { Resource.Loading() }
        openDocumentLauncher?.launch(mimeTypes.toTypedArray())
        val res = requestResource.waitUntilLoadedOrError()
        return res
    }
}
