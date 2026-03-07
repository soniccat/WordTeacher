package com.aglushkov.wordteacher.shared.general.serialization

import com.aglushkov.wordteacher.shared.general.extensions.updateWithLoadedData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import com.aglushkov.wordteacher.shared.workers.SerialQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.Path
import kotlin.reflect.KType

class SerializableFileCache<T>(
    kType: KType,
    private val filePath: Path,
    private val fileSystem: FileSystem,
) {
    private val queue = SerialQueue(Dispatchers.IO)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val stateFlow = MutableStateFlow<Resource<T>>(Resource.Uninitialized())

    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val sr = json.serializersModule.serializer(kType)

    init {
        scope.launch {
            load()
        }
    }

    fun set(obj: T) {
        stateFlow.updateWithLoadedData(obj)
        queue.sendWithDelay("put", 1000) {
            save()
        }
    }

    private fun load() {
        return queue.send {
            if (fileSystem.exists(filePath)) {
                loadResource {
                    val text = fileSystem.read(filePath) {
                        readString(Charsets.UTF_8)
                    }
                    json.decodeFromString<T>(sr as DeserializationStrategy<T>, text)
                }.collect(stateFlow)
            }
        }
    }

    fun save() {
        scope.launch {
            stateFlow.value.data()?.let {
                val jsonString = json.encodeToString<T>(sr, it)
                fileSystem.write(filePath) {
                    writeString(jsonString, Charsets.UTF_8)
                }
            }
        }
    }
}