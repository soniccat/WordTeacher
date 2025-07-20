package com.aglushkov.wordteacher.android_app.helper

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import okio.Path
import okio.sink
import okio.source
import java.io.File
import java.util.UUID


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
    override val state: MutableStateFlow<Resource<Unit>> = MutableStateFlow(Resource.Uninitialized())

//    @SuppressLint("Recycle")
    fun bind(activity: ComponentActivity) {
        openDocumentLauncher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { result ->
            mainScope.launch(Dispatchers.IO) {
                if (result == null) {
                    state.update { it.toUninitialized() }
                } else {
                    loadResource {
                        val name = activity.resolveFileName(result) ?: UUID.randomUUID().toString()
                        val tmpFilePath = if (tmpPath.toFile().isDirectory) {
                            tmpPath.div(name)
                        } else {
                            tmpPath
                        }
                        val dstFilePath = if (dstPath.toFile().isDirectory) {
                            dstPath.div(name)
                        } else {
                            dstPath
                        }

                        tmpFilePath.useAsTmp {
                            activity.contentResolver.openInputStream(result)?.source()
                                ?.writeTo(it.toFile().sink())
                            validator.validateFile(it)

                            it.toFile().source()?.writeTo(dstFilePath.toFile().sink())
                            successHandler.handle(dstFilePath)
                        }
                        Unit
                    }.collect(state)
                }
            }
        }
    }

    override suspend fun chooseFile(): Resource<Unit> {
        state.update { Resource.Loading() }
        openDocumentLauncher?.launch(mimeTypes.toTypedArray())
        return state.value
    }
}

fun Context.resolveFileName(uri: Uri): String? = when(uri.scheme) {
    ContentResolver.SCHEME_CONTENT -> getContentFileName(uri)
    else -> uri.path?.let(::File)?.name
}

private fun Context.getContentFileName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        cursor.moveToFirst()
        return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
    }
}.getOrNull()