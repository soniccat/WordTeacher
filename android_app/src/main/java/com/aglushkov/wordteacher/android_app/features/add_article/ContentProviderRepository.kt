package com.aglushkov.wordteacher.android_app.features.add_article

import android.content.Context
import android.net.Uri
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.RuntimeException

class ContentProviderRepository(
    private val context: Context,
): SimpleResourceRepository<String, String>() {
    override suspend fun load(arg: String): String = withContext(Dispatchers.Default) {
        context.contentResolver.openInputStream(Uri.parse(arg))?.buffered()?.use {
            it.readBytes().decodeToString()
        } ?: run { throw RuntimeException("wrong argument $arg") }
    }
}
