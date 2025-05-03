package com.aglushkov.wordteacher.android_app.features.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.aglushkov.wordteacher.shared.features.settings.vm.FileSharer
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okio.Path
import java.io.File


class FileSharerRepository(
    private val context: Context,
): SimpleResourceRepository<Unit, Path>() {
    override suspend fun load(arg: Path) {
        val imagePath = File(context.filesDir, arg.parent?.name.orEmpty())
        val newFile = File(imagePath, arg.name)
        val contentUri: Uri = FileProvider.getUriForFile(context, "com.aglushkov.fileprovider", newFile)
        val intent = ShareCompat.IntentBuilder(context)
            .setStream(contentUri)
            .setType("text/*")
            .intent
            .setAction(Intent.ACTION_SEND)
            .setDataAndType(contentUri, "text/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

        startActivity(context, intent, null)
    }
}

fun FileSharerRepository.toFileSharer(): FileSharer {
    return object : FileSharer {
        override fun share(path: Path): Flow<Resource<Unit>> {
            return load(path, Resource.Uninitialized())
        }
    }
}
