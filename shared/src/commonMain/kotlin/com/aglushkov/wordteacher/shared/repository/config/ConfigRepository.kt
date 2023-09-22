package com.aglushkov.wordteacher.shared.repository.config

import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isNotLoadedAndNotLoading
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

class ConfigRepository(
    private val configPath: Path,
    private val fileSystem: FileSystem,
    private val wordTeacherDictServiceConfig: Config,
) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<List<Config>>>(Resource.Loaded(listOf(wordTeacherDictServiceConfig)))
    val flow = stateFlow
    val value: Resource<List<Config>>
        get() {
            return stateFlow.value
        }

    private val configJson by lazy {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }
    }

    init {
        mainScope.launch {
            if (fileSystem.exists(configPath)) {
                load()
            }
        }
    }

    suspend fun load() {
        loadResource {
            fileSystem.read(configPath) {
                val byteArray = readByteArray()
                // TODO: decifer
                listOf(wordTeacherDictServiceConfig) + configJson.decodeFromString<List<Config>>(byteArray.decodeToString())
            }
        }.forward(stateFlow)
    }

    fun save(configs: List<Config>) {
        fileSystem.write(configPath) {
            val str = configJson.encodeToString(configs)
            val byteArray = str.toByteArray()
            // TODO: cifer
            write(byteArray)
        }
        stateFlow.update { Resource.Loaded(configs) }
    }

    fun putConfig(config: Config) {
        val configs = stateFlow.updateAndGet { configListRes ->
            val configs = configListRes.data().orEmpty().let { configs ->
                configs.indexOfFirst { it.type == config.type }.let { configIndex ->
                    if (configIndex == -1) {
                        configs + config
                    } else {
                        configs.toMutableList().apply {
                            this[configIndex] = config
                        }
                    }
                }
            }
            configListRes.toLoaded(configs)
        }
        save(configs.data().orEmpty())
    }

    fun removeConfig(type: Config.Type) {
        val configs = stateFlow.updateAndGet { configListRes ->
            configListRes.transform { configs ->
                configs.filter { it.type != type }
            }
        }
        save(configs.data().orEmpty())
    }
}

private const val TAG = "ConfigRepository"