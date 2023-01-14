package com.aglushkov.wordteacher.shared.repository.space

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.asLoaded
import com.aglushkov.wordteacher.shared.general.resource.isNotLoadedAndNotLoading
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.service.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import okio.FileSystem
import okio.Path

class SpaceAuthRepository(
    private val service: SpaceAuthService,
    private val cachePath: Path,
    private val fileSystem: FileSystem,
) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<AuthData>>(Resource.Uninitialized())
    val authDataFlow: Flow<Resource<AuthData>> = stateFlow
    val value: Resource<AuthData>
    get() {
        return stateFlow.value
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    init {
        mainScope.launch(Dispatchers.Default) {
            restore()?.let { authData ->
                stateFlow.compareAndSet(Resource.Uninitialized(), Resource.Loaded(authData))
            }
        }
    }

    private fun store(authData: AuthData) {
        fileSystem.write(cachePath) {
            write(json.encodeToString(authData).toByteArray())
        }
    }

    private fun restore(): AuthData? {
        if (!fileSystem.exists(cachePath)) {
            return null
        }

        return try {
            fileSystem.read(cachePath) {
                json.decodeFromString<AuthData>(readByteString().utf8())
            }
        } catch (e: Exception) {
            null
        }
    }

    fun auth(network: SpaceAuthService.NetworkType, token: String) {
        mainScope.launch {
            authFlow(network, token).onStart {
                stateFlow.value.toLoading()
            }.onEach {
                it.asLoaded()?.data?.let { authData ->
                    launch(Dispatchers.Default) {
                        store(authData)
                    }
                }
            }.forward(stateFlow)
        }
    }

    private fun authFlow(network: SpaceAuthService.NetworkType, token: String) = flow {
        try {
            val authData = service.auth(network, token).ToOkResult()
            emit(Resource.Loaded(authData))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(e.message.orEmpty(), TAG)
            e.printStackTrace()
            emit(stateFlow.value.toError(e, true))
        }
    }
}

private const val TAG = "SpaceAuthRepository"
