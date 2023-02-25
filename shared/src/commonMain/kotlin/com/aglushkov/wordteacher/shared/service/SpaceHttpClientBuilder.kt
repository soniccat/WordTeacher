package com.aglushkov.wordteacher.shared.service

import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.asLoaded
import com.aglushkov.wordteacher.shared.general.resource.isError
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.repository.deviceid.DeviceIdRepository
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import io.ktor.client.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class SpaceHttpClientBuilder(
    private val deviceIdRepository: DeviceIdRepository,
    private val appInfo: AppInfo,
    private val cookieStorage: CookiesStorage,
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
                        spaceAuthRepositoryProvider().currentAuthData.asLoaded()?.data?.let { authData ->
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

                    val spaceRepository = spaceAuthRepositoryProvider()
                    if (spaceRepository.isAuthUrl(originalCall.request.url)) {
                        return@on originalCall // handle them SpaceAuthRepository
                    }

                    return@on originalCall.response.run { // this: HttpResponse
                        val oldAutData = spaceRepository.currentAuthData.asLoaded()

                        if(status == HttpStatusCode.Unauthorized) {
                            try {
                                var result: Resource<AuthData> = Resource.Uninitialized()

                                // if we have valid cookies then we can call refresh
                                val session = cookieStorage.get(this.request.url).firstOrNull { it.name == CookieSession }?.value
                                if (session != null) {
                                    result = spaceRepository.refresh()
                                }

                                val call = if (!result.isLoaded() && result.errorStatusCode() == HttpStatusCode.Unauthorized.value) {
                                    // wordteacher space token is outdated
                                    spaceRepository.networkType?.let { networkType ->
                                        spaceRepository.signIn(networkType)
                                    }

                                    val newAutData = spaceRepository.currentAuthData.asLoaded()
                                    if (newAutData?.data?.user == oldAutData?.data?.user) {
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
}
