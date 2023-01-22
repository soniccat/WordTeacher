package com.aglushkov.wordteacher.shared.repository.space

import com.aglushkov.wordteacher.shared.general.toOkResult
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.asLoaded
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import com.aglushkov.wordteacher.shared.service.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
            loadResource(stateFlow.value) {
                service.auth(network, token).toOkResult()
            }.onEach {
                it.asLoaded()?.data?.let { authData ->
                    launch(Dispatchers.Default) {
                        store(authData)
                    }
                }
            }.collect(stateFlow)
        }
    }

    fun signOut(network: SpaceAuthService.NetworkType) {
        val authData = stateFlow.value.asLoaded()?.data ?: return
        if (authData.user.networkType == network) {
            stateFlow.value = Resource.Uninitialized(version = 1) // version = 1 to distinguish this Uninitialized from a default one
        }
    }

    fun refresh() {
        val authDataRes = stateFlow.value.asLoaded() ?: return
        mainScope.launch {
            loadResource(authDataRes.copyWith(authDataRes.data.authToken)) {
                service.refresh(authDataRes.data.authToken).toOkResult()
            }.map { newTokenRes ->
                newTokenRes.transform(authDataRes) { newToken ->
                    authDataRes.data.copy(authToken = newToken)
                }
            }.collect(stateFlow)
        }
    }
}

private const val TAG = "SpaceAuthRepository"
