package com.aglushkov.wordteacher.shared.repository.dict

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.general.resource.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class DictRepository(
    private val path: Path,
    private val dictFactory: DictFactory,
    private val fileSystem: FileSystem
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val dicts = MutableStateFlow<Resource<List<Dict>>>(Resource.Uninitialized())

    init {
        importDicts()
    }

    fun importDicts() {
        scope.launch {
            importDictsInternal()
        }
    }

    private suspend fun importDictsInternal() {
        val currentDicts = dicts
        fileSystem.listOrNull(path)?.onEach { filePath ->
            val isDictLoaded = currentDicts.value.data()?.firstOrNull {
                it.path == filePath
            } != null
            if (!isDictLoaded) {
                val newDict = dictFactory.createDict(filePath)
            }
        }
    }
}