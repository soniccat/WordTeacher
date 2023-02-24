package com.aglushkov.wordteacher.shared.repository.space

import com.aglushkov.wordteacher.shared.general.ErrorResponseException
import com.aglushkov.wordteacher.shared.general.GoogleAuthRepository
import com.aglushkov.wordteacher.shared.general.resource.*
import com.aglushkov.wordteacher.shared.general.toOkResult
import com.aglushkov.wordteacher.shared.service.*
import io.ktor.http.*
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
    private val googleAuthRepository: GoogleAuthRepository,
    private val cachePath: Path,
    private val fileSystem: FileSystem,
) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val spaceAuthStateFlow = MutableStateFlow<Resource<AuthData>>(Resource.Uninitialized())

    private val stateFlow: StateFlow<Resource<AuthData>> = combine(
        googleAuthRepository.googleAuthDataFlow,
        spaceAuthStateFlow
    ) { googleRes, spaceRes ->
        googleRes.merge(spaceRes) { gr, sr ->
            sr
        }
    }.stateIn(mainScope, SharingStarted.Eagerly, Resource.Uninitialized())

    val authDataFlow: Flow<Resource<AuthData>> = stateFlow
    val currentAuthData: Resource<AuthData>
    get() {
        return stateFlow.value
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

    suspend fun signIn(network: SpaceAuthService.NetworkType): Resource<AuthData> {
        if (network != SpaceAuthService.NetworkType.Google) {
            throw RuntimeException("authInternal: unsupported network")
        }

        val googleAuthDataRes = googleAuthRepository.googleAuthDataFlow.value
        if (googleAuthDataRes.isLoading()) {
            return currentAuthData
        }

        val googleAuthData = if (!googleAuthDataRes.isLoaded()) {
            googleAuthRepository.signIn()
        } else {
            googleAuthDataRes
        }

        googleAuthData.asLoaded()?.let { loadedGoogleAuthData ->
            val authResult = auth(network, loadedGoogleAuthData.data.tokenId)
            authResult.asError()?.let { errorAuthData ->
                (errorAuthData.throwable as? ErrorResponseException)?.let {
                    // id token is expired, need to resign-in
                    if (it.statusCode == HttpStatusCode.Unauthorized.value) {
                        val googleAuthData2 = googleAuthRepository.signIn()
                        // on error just keep error in googleAuthRepository
                        googleAuthData2.asLoaded()?.let { loadedGoogleAuthData2 ->
                            // try second time
                            auth(network, loadedGoogleAuthData2.data.tokenId)
                        }
                    }
                }
            }
        }
        // on error just keep error in googleAuthRepository

        return currentAuthData
    }

    fun launchSignIn(network: SpaceAuthService.NetworkType, token: String) {
        mainScope.launch {
            auth(network, token)
        }
    }

    private suspend fun auth(
        network: SpaceAuthService.NetworkType,
        token: String
    ): Resource<AuthData> {
        var res: Resource<AuthData> = stateFlow.value
        loadResource(res) {
            service.auth(network, token).toOkResult()
        }.onEach {
            storeAuthDataIfNeeded(it)
            res = it
        }.collect(spaceAuthStateFlow)

        return res
    }

    fun signOut(network: SpaceAuthService.NetworkType) {
        if (spaceAuthStateFlow.value.data()?.user?.networkType == network) {
            spaceAuthStateFlow.value = Resource.Uninitialized(version = 1) // version = 1 to distinguish this Uninitialized from a default one
        }

        if (network == SpaceAuthService.NetworkType.Google) {
            googleAuthRepository.launchSignOut()
        }
    }

    fun launchRefresh() {
        mainScope.launch {
            refresh()
        }
    }

    suspend fun refresh(): Resource<AuthData> {
        var stateValue = stateFlow.value
        val authDataRes = stateValue.asLoaded() ?: return stateValue

        loadResource(authDataRes.copyWith(authDataRes.data.authToken)) {
            service.refresh(authDataRes.data.authToken).toOkResult()
        }.map { newTokenRes ->
            newTokenRes.transform(authDataRes) { newToken ->
                authDataRes.data.copy(authToken = newToken)
            }
        }.onEach {
            storeAuthDataIfNeeded(it)
            stateValue = it
        }.collect(spaceAuthStateFlow)

        return stateValue
    }

    private fun storeAuthDataIfNeeded(it: Resource<AuthData>) {
        it.asLoaded()?.data?.let { authData ->
            mainScope.launch(Dispatchers.Default) {
                store(authData)
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

    fun isAuthUrl(url: Url) = service.isAuthUrl(url)
}

private const val TAG = "SpaceAuthRepository"
