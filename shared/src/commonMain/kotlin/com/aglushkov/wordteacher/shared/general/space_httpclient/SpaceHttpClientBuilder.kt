package com.aglushkov.wordteacher.shared.general.space_httpclient

import com.aglushkov.wordteacher.shared.general.AppInfo
import com.aglushkov.wordteacher.shared.general.GoogleAuthRepository
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilLoadedOrError
import com.aglushkov.wordteacher.shared.general.getUserAgent
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.asLoaded
import com.aglushkov.wordteacher.shared.general.resource.isError
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.repository.deviceid.DeviceIdRepository
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.service.*
import io.ktor.client.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class SpaceHttpClientBuilder(
    private val deviceIdRepository: DeviceIdRepository,
    private val appInfo: AppInfo,
    private val cookieStorage: CookiesStorage,
    private val googleAuthRepositoryProvider: () -> GoogleAuthRepository,
    private val spaceAuthRepositoryProvider: () -> SpaceAuthRepository,
    private val isDebug: Boolean
) {
    fun build() = HttpClient {
        if (isDebug) {
            installLogger()
        }
        installCookies()
        installHeaders()
        install401Handler()
    }

    private fun HttpClientConfig<*>.installLogger() {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    com.aglushkov.wordteacher.shared.general.Logger.v(message, "SpaceHttpClient")
                }
            }
            level = LogLevel.ALL
        }
    }

    private fun HttpClientConfig<*>.installCookies() {
        install(HttpCookies) {
            storage = cookieStorage
        }
    }

    private fun HttpClientConfig<*>.installHeaders() {
        install(
            createClientPlugin("SpacePlugin") {
                onRequest { request, _ ->
                    request.headers {
                        set(HeaderDeviceType, "android")
                        set(HeaderDeviceId, deviceIdRepository.deviceId())
                        spaceAuthRepositoryProvider().value.asLoaded()?.data?.let { authData ->
                            set(HeaderAccessToken, authData.authToken.accessToken)
                        }
                        set(HttpHeaders.UserAgent, appInfo.getUserAgent())
                    }
                }
            }
        )
    }

    private fun HttpClientConfig<*>.install401Handler() {
        install(
            createClientPlugin("AuthInterceptor for 401 error") {
                on(Send) { request ->
                    val originalCall = proceed(request)
                    originalCall.response.run { // this: HttpResponse
                        val googleAuthRepository = googleAuthRepositoryProvider()
                        val spaceRepository = spaceAuthRepositoryProvider()
                        val oldAutData = spaceRepository.value.asLoaded()

                        if(status == HttpStatusCode.Unauthorized) {
                            try {
                                var result: Resource<AuthData> = Resource.Uninitialized()

                                // if we have valid cookies then we can call refresh
                                val session = cookieStorage.get(this.request.url).firstOrNull { it.name == CookieSession }?.value
                                val isRefreshUrl = this.request.url.isAuthRefreshUrl()
                                if (session != null && !isRefreshUrl) {
                                    result = spaceRepository.refresh()
                                }

                                val call = if (!result.isLoaded()) {
                                    // try to reauth
                                    if (googleAuthRepository.googleSignInCredentialFlow.value.isError() || googleAuthRepository.googleSignInCredentialFlow.value.isUninitialized()) {
                                        googleAuthRepository.signIn()
                                        googleAuthRepository.googleSignInCredentialFlow.waitUntilLoadedOrError()
                                        spaceRepository.authDataFlow.waitUntilLoadedOrError()
                                    } else {
                                        googleAuthRepository.googleSignInCredentialFlow.waitUntilLoadedOrError()
                                        googleAuthRepository.googleSignInCredentialFlow.value.asLoaded()?.data?.tokenId?.let { tokenId ->
                                            spaceRepository.auth(SpaceAuthService.NetworkType.Google, tokenId)
                                        }
                                    }

                                    val newAutData = spaceRepository.value.asLoaded()
                                    if (!isRefreshUrl && newAutData?.data?.user == oldAutData?.data?.user) {
                                        proceed(request)
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }
                                call ?: originalCall
                            } catch (e: Exception) {
                                originalCall
                            }
                        } else {
                            originalCall
                        }
                    }
                }
            }
        )
    }

    private fun Url.isAuthRefreshUrl(): Boolean {
        return pathSegments.size >= 2 && pathSegments.subList(pathSegments.size - 2, pathSegments.size) == listOf("auth", "refresh")
    }
}
