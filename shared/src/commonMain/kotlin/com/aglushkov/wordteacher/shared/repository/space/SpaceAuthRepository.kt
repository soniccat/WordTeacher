package com.aglushkov.wordteacher.shared.repository.space

import com.aglushkov.wordteacher.shared.general.ErrorResponseException
import com.aglushkov.wordteacher.shared.general.auth.GoogleAuthController
import com.aglushkov.wordteacher.shared.general.auth.VKAuthController
import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthController
import com.aglushkov.wordteacher.shared.general.resource.*
import com.aglushkov.wordteacher.shared.general.toOkResponse
import com.aglushkov.wordteacher.shared.service.*
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
import io.ktor.http.*
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

class SpaceAuthRepository(
    private val service: SpaceAuthService,
    private val googleAuthController: GoogleAuthController,
    private val vkAuthController: VKAuthController,
    private val cachePath: Path,
    private val fileSystem: FileSystem,
    private val databaseCardWorker: () -> DatabaseCardWorker,
) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val spaceAuthStateFlow = MutableStateFlow<Resource<SpaceAuthData>>(Resource.Uninitialized())

    val authDataFlow: Flow<Resource<SpaceAuthData>> = spaceAuthStateFlow
    val currentAuthData: Resource<SpaceAuthData>
    get() {
        return spaceAuthStateFlow.value
    }

    val networkType: SpaceAuthService.NetworkType?
    get() {
        return currentAuthData.data()?.user?.networkType
    }

    fun isAuthorized(): Boolean {
        return currentAuthData.asLoaded() != null
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    init {
        mainScope.launch(Dispatchers.Default) {
            restore()?.let { authData ->
                spaceAuthStateFlow.compareAndSet(Resource.Uninitialized(), Resource.Loaded(authData))
            }
        }
    }

    fun launchReauth()  {
        spaceAuthStateFlow.value.asLoaded()?.data?.let { loadedAuthData ->
            loadedAuthData.user.networkType?.let { networkType ->
                launchSignIn(networkType)
            }
        }
    }

    fun launchSignIn(network: SpaceAuthService.NetworkType) {
        mainScope.launch {
            signIn(network)
        }
    }

    suspend fun signIn(network: SpaceAuthService.NetworkType): Resource<SpaceAuthData> {
        val authController = resolveAuthController(network) ?: throw RuntimeException("authInternal: unsupported network")

        if (spaceAuthStateFlow.isLoading()) { // TODO: looks weird, consider removing
            spaceAuthStateFlow.takeWhile { it.isNotLoading() }.collect()
        }

        spaceAuthStateFlow.value = Resource.Loading()

        val authDataRes = authController.authDataFlow.value
        val authData = if (!authDataRes.isLoaded()) {
            withContext(Dispatchers.Main) {
                authController.signIn()
            }
        } else {
            authDataRes
        }

        var authResult: Resource<SpaceAuthData> = Resource.Uninitialized()
        authData.asLoaded()?.let { loadedAuthData ->
            authResult = auth(network, loadedAuthData.data.token)
            authResult.asError()?.let { errorAuthData ->
                (errorAuthData.throwable as? ErrorResponseException)?.let {
                    // id token is expired, need to resign-in
                    if (it.statusCode == HttpStatusCode.Unauthorized.value) {
                        val authData2 = authController.signIn()
                        // on error just keep error in authController
                        authData2.asLoaded()?.let { loadedAuthData2 ->
                            // try second time
                            authResult = auth(network, loadedAuthData2.data.token)
                        }
                    }
                }
            }
        }
        // on error just keep error in authController

        spaceAuthStateFlow.value = authResult

        return currentAuthData
    }

    private fun resolveAuthController(network: SpaceAuthService.NetworkType): NetworkAuthController? =
        when (network) {
            SpaceAuthService.NetworkType.Google -> googleAuthController
            SpaceAuthService.NetworkType.VKID -> vkAuthController
        }

    private suspend fun auth(network: SpaceAuthService.NetworkType, token: String): Resource<SpaceAuthData> {
        return loadResource(currentAuthData) {
            service.auth(network, token).toOkResponse()
        }.onEach {
            storeAuthDataIfNeeded(it)
        }.last()
    }

    fun signOut(network: SpaceAuthService.NetworkType) {
        mainScope.launch {
            databaseCardWorker().waitUntilSyncIsDone()
            if (spaceAuthStateFlow.value.data()?.user?.networkType == network) {
                fileSystem.delete(cachePath)
                spaceAuthStateFlow.value =
                    Resource.Uninitialized(version = 1) // version = 1 to distinguish this Uninitialized from a default one

                val authController = resolveAuthController(network)
                authController?.launchSignOut()
            }
        }
    }

    fun launchRefresh() {
        mainScope.launch {
            refresh()
        }
    }

    suspend fun refresh(): Resource<SpaceAuthData> {
        var stateValue = currentAuthData
        val authDataRes = stateValue.asLoaded() ?: return stateValue

        loadResource(authDataRes.copyWith(authDataRes.data.authToken)) {
            service.refresh(authDataRes.data.authToken).toOkResponse()
        }.map { newTokenRes ->
            newTokenRes.mapLoadedData { newToken ->
                authDataRes.data.copy(authToken = newToken)
            }
        }.onEach {
            storeAuthDataIfNeeded(it)
            stateValue = it
        }.collect(spaceAuthStateFlow)

        return stateValue
    }

    private fun storeAuthDataIfNeeded(it: Resource<SpaceAuthData>) {
        it.asLoaded()?.data?.let { authData ->
            mainScope.launch(Dispatchers.Default) {
                store(authData)
            }
        }
    }

    private fun store(authData: SpaceAuthData) {
        fileSystem.write(cachePath) {
            write(json.encodeToString(authData).toByteArray())
        }
    }

    private fun restore(): SpaceAuthData? {
        if (!fileSystem.exists(cachePath)) {
            return null
        }

        return try {
            fileSystem.read(cachePath) {
                json.decodeFromString<SpaceAuthData>(readByteString().utf8())
            }
        } catch (e: Exception) {
            null
        }
    }

    fun isAuthUrl(url: Url) = service.isAuthUrl(url)
}

private const val TAG = "SpaceAuthRepository"
