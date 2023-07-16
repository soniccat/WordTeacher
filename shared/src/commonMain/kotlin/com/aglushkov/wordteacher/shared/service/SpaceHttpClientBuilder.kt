package com.aglushkov.wordteacher.shared.service

import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.resource.*
import com.aglushkov.wordteacher.shared.general.serialization.GZip
import com.aglushkov.wordteacher.shared.repository.deviceid.DeviceIdRepository
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import io.ktor.client.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*

class SpaceHttpClientBuilder(
    private val deviceIdRepository: DeviceIdRepository,
    private val appInfo: AppInfo,
    private val cookieStorage: CookiesStorage,
    private val spaceAuthRepositoryProvider: () -> SpaceAuthRepository,
    private val platform: String,
    private val isDebug: Boolean,
) {
    fun build() = HttpClient {
        if (isDebug) {
            installLogger()
        }
        installGzipForResponse()
//        installGzipForRequest()
        installCookies()
        installHeaders()
        install401Handler()
    }

    private fun HttpClientConfig<*>.installGzipForResponse() {
        install(ContentEncoding) {
            gzip()
        }
    }

    private fun HttpClientConfig<*>.installGzipForRequest() {
        val gzip = GZip()

        install(
            createClientPlugin("Gzip for request") {
                transformRequestBody { request, content, bodyType ->
                    if (content is String) {
                        ByteArrayContent(
                            bytes = gzip.compress(content),
                            contentType = ContentType.Application.GZip,
                        )
                    } else {
                        null
                    }
                }
            }
        )
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
                        set(HeaderDeviceType, platform)
                        set(HeaderDeviceId, deviceIdRepository.deviceId())
                        spaceAuthRepositoryProvider().currentAuthData.asLoaded()?.data?.let { authData ->
                            request.setAuthData(authData)
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

                                val newCall = if (result.isLoaded()) {
                                    proceed(request.setAuthData(result.data()!!))

                                } else if (result.isUninitialized() || result.errorStatusCode() == HttpStatusCode.Unauthorized.value) {
                                    // wordteacher space token is outdated, need to sign-in again
                                    spaceRepository.networkType?.let { networkType ->
                                        spaceRepository.signIn(networkType)
                                    }

                                    val newAutData = spaceRepository.currentAuthData.asLoaded()
                                    if (newAutData?.data?.user == oldAutData?.data?.user) {
                                        val newCookies = cookieStorage.get(this.request.url)
                                        request.headers {
                                            set(HttpHeaders.Cookie, renderClientCookies(newCookies))
                                        }
                                        com.aglushkov.wordteacher.shared.general.Logger.v("newAuthData ${newAutData?.data}", tag = "SpaceHttpClientBuilder")
                                        proceed(request.setAuthData(newAutData?.data!!))
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }
                                newCall ?: originalCall
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

    private fun HttpRequestBuilder.setAuthData(authData: AuthData): HttpRequestBuilder {
        headers {
            set(HeaderAccessToken, authData.authToken.accessToken)
        }
        return this
    }
}

private fun renderClientCookies(cookies: List<Cookie>): String =
    cookies.joinToString("; ", transform = ::renderCookieHeader)