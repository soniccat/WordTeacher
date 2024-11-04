package com.aglushkov.wordteacher.shared.repository.worddefinition

import com.aglushkov.wordteacher.shared.general.extensions.updateLoadedData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.workers.SerialQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path

class WordDefinitionHistoryRepository(
    private val historyPath: Path,
    private val fileSystem: FileSystem,
) {
    private val queue = SerialQueue(Dispatchers.Default)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val stateFlow = MutableStateFlow<Resource<List<String>>>(Resource.Uninitialized())

    init {
        scope.launch {
            load()
        }
    }

    private fun load() {
        return queue.send {
            fileSystem.read(historyPath) {
                val words = mutableListOf<String>()
                while(!exhausted()) {
                    val word = readUtf8Line()
                    if (word?.isNotEmpty() == true) {
                        words.add(word)
                    }
                }
                stateFlow.update { Resource.Loaded(words) }
            }
        }
    }

    fun put(word: String) {
        stateFlow.updateLoadedData { words ->
            (listOf(word) + words.filter { it != word }).take(HISTORY_MAX)
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
            words.filter { it != word }
        }
    }
}

private const val HISTORY_MAX = 300
