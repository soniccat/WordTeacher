package com.aglushkov.wordteacher.shared.service

import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.resource.*
import com.aglushkov.wordteacher.shared.general.serialization.GZip
import com.aglushkov.wordteacher.shared.repository.deviceid.DeviceIdRepository
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
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
import io.ktor.util.decodeBase64Bytes

class SpaceHttpClientBuilder(
    private val deviceIdRepository: DeviceIdRepository,
    private val appInfo: AppInfo,
    private val cookieStorage: CookiesStorage,
    private val spaceAuthRepositoryProvider: () -> SpaceAuthRepository,
    private val secureCodec: SecureCodec,
    private val isDebug: Boolean,
) {
    fun build() = HttpClient {
        installLogger()
        installGzipForResponse()
//        installGzipForRequest()
        installCookies()
        installHeaders()
        install401Handler()
    }

    private fun HttpClientConfig<*>.installGzipForResponse() {
        install(ContentEncoding) {
            gzip(0.9F)
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
            if (!isDebug) {
                filter {
                    it.url.pathSegments.all { it != "auth" }
                }
                sanitizeHeader {
                    header -> header == HeaderAccessToken
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
                        set(HeaderDeviceType, appInfo.osName)
                        set(HeaderDeviceId, deviceIdRepository.deviceId())
                        set(HeaderAppVersion, appInfo.version)
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
                                // try to refresh, if refresh token isn't expired a new access token and a new refresh token will be granted
                                val refreshResult = spaceRepository.refresh()
                                val newCall = if (refreshResult.isLoaded()) {
                                    proceed(request.setAuthData(refreshResult.data()!!))

                                } else if (refreshResult.isUninitialized() || refreshResult.errorStatusCode() == HttpStatusCode.Unauthorized.value) {
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

    private fun HttpRequestBuilder.setAuthData(authData: SpaceAuthData): HttpRequestBuilder {
        headers {
            set(
                HeaderAccessToken,
                secureCodec.decrypt(authData.authToken.accessToken.decodeBase64Bytes())!!.decodeToString(),
            )
        }
        return this
    }
}

private fun renderClientCookies(cookies: List<Cookie>): String =
    cookies.joinToString("; ", transform = ::renderCookieHeader)