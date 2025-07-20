package com.aglushkov.wordteacher.shared.repository.history

import com.aglushkov.wordteacher.shared.general.extensions.updateLoadedData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import com.aglushkov.wordteacher.shared.general.resource.onLoaded
import com.aglushkov.wordteacher.shared.workers.SerialQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path

open class HistoryRepository(
    private val historyPath: Path,
    private val fileSystem: FileSystem,
    private val limit: Int,
) {
    private val queue = SerialQueue(Dispatchers.IO)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val stateFlow = MutableStateFlow<Resource<LinkedHashSet<String>>>(Resource.Uninitialized())
    init {
        scope.launch {
            load()
        }
    }

    private fun load() {
        return queue.send {
            loadResource {
                val words = mutableListOf<String>()
                if (fileSystem.exists(historyPath)) {
                    fileSystem.read(historyPath) {
                        while (!exhausted()) {
                            val word = readUtf8Line()
                            if (word?.isNotEmpty() == true) {
                                words.add(word)
                            }
                        }
                    }
                }
                LinkedHashSet(words)
            }.collect(stateFlow)
        }
    }

    fun put(word: String) {
        stateFlow.updateLoadedData { words ->
            val newList = (listOf(word) + words.filter { it != word }).take(limit)
            LinkedHashSet(newList)
        }
        queue.sendWithDelay("put", 1000) {
            save()
        }
    }

    fun save() {
        scope.launch {
            fileSystem.write(historyPath) {
                stateFlow.value.data()?.let { words ->
                    words.onEach {
                        writeUtf8(it + "\n")
                    }
                }
            }
        }
    }

    fun delete(word: String) {
        stateFlow.updateLoadedData { words ->
            val newList = words.filter { it != word }
            LinkedHashSet(newList)
        }
    }

    fun contains(word: String): Boolean {
        return stateFlow.value.data().orEmpty().contains(word)
    }
}