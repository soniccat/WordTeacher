package com.aglushkov.wordteacher.shared.repository.config

import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isError
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import io.ktor.util.encodeBase64
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

class ConfigRepository(
    private val configPath: Path,
    private val fileSystem: FileSystem,
    private val wordTeacherDictServiceConfig: Config,
    private val secureCodec: SecureCodec,
) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<List<Config>>>(Resource.Uninitialized())
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
            } else {
                save(listOf(wordTeacherDictServiceConfig))
            }
        }
    }

    suspend fun load() {
        loadResource {
            fileSystem.read(configPath) {
                val byteArray = readByteArray()
                configJson.decodeFromString<List<Config>>(byteArray.decodeToString())
            }
        }.map {
            if (it.isError()) {
                Resource.Loaded(listOf(wordTeacherDictServiceConfig))
            } else {
                it
            }
        }.forward(stateFlow)
    }

    fun save(configs: List<Config>) {
        val encryptedConfigs = configs.map {
            it.copy(
                connectParams = if (it.connectParams.key.isNotEmpty()) {
                    it.connectParams.copy(
                        key = "",
                        securedKey = secureCodec.encrypt(it.connectParams.key.toByteArray()).encodeBase64()
                    )
                } else {
                    it.connectParams
                }
            )
        }
        fileSystem.write(configPath) {
            val str = configJson.encodeToString(encryptedConfigs)
            val byteArray = str.toByteArray()
            write(byteArray)
        }
        stateFlow.update { Resource.Loaded(encryptedConfigs) }
    }

    fun addConfig(config: Config) {
        val configs = stateFlow.updateAndGet { configListRes ->
            val configs = configListRes.data().orEmpty() + config
            configListRes.toLoaded(configs)
        }
        save(configs.data().orEmpty())
    }

    fun removeConfig(id: Int) {
        val configs = stateFlow.updateAndGet { configListRes ->
            configListRes.mapLoadedData { configs ->
                configs.filter { it.id != id }
            }
        }
        save(configs.data().orEmpty())
    }

    fun updateConfig(config: Config) {
        val configs = stateFlow.updateAndGet { configListRes ->
            configListRes.mapLoadedData { configs ->
                configs.map {
                    if (it.id == config.id) {
                        config
                    } else {
                        it
                    }
                }
            }
        }
        save(configs.data().orEmpty())
    }
}

private const val TAG = "ConfigRepository"
