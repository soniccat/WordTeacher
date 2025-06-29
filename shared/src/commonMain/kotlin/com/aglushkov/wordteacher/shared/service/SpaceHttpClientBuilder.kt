package com.aglushkov.wordteacher.shared.service

import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.synchronize
import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.resource.*
import com.aglushkov.wordteacher.shared.general.serialization.GZip
import com.aglushkov.wordteacher.shared.repository.deviceid.DeviceIdRepository
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilFalse
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
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.internal.synchronized

class SpaceHttpClientBuilder(
    private val deviceIdRepository: DeviceIdRepository,
    private val appInfo: AppInfo,
    private val cookieStorage: CookiesStorage,
    private val spaceAuthRepositoryProvider: () -> SpaceAuthRepository,
    private val secureCodec: SecureCodec,
    private val isDebug: Boolean,
    private val analyticsProvider: () -> Analytics,
) {
    fun build() = HttpClient {
        installLogger(isDebug)
        installGzipForResponse()
//        installGzipForRequest()
        installCookies()
        installHeaders()
        install401Handler()
        installErrorTracker(analyticsProvider)
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

    private val unauthHandlingIsInProgress = MutableStateFlow(false)
    private fun HttpClientConfig<*>.install401Handler() {
        install(
            createClientPlugin("AuthInterceptor for 401 error") {
                on(Send) { request ->
                    val originalCall = proceed(request)

                    val spaceRepository = spaceAuthRepositoryProvider()
                    if (spaceRepository.isAuthUrl(originalCall.request.url)) {
                        return@on originalCall // handle them SpaceAuthRepository
                    }

                    return@on originalCall.response.run {
                        val oldAutData = spaceRepository.currentAuthData.asLoaded()

                        if(status == HttpStatusCode.Unauthorized) {
                            try {
                                if (unauthHandlingIsInProgress.compareAndSet(false, true)) {
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
                                            proceed(request.setAuthData(newAutData?.data!!))
                                        } else {
                                            null
                                        }
                                    } else {
                                        null
                                    }

                                    unauthHandlingIsInProgress.compareAndSet(true, false)
                                    newCall ?: originalCall
                                } else {
                                    unauthHandlingIsInProgress.waitUntilFalse()

                                    val newAutData = spaceRepository.currentAuthData.asLoaded()
                                    val newCall = if (newAutData?.data?.user == oldAutData?.data?.user) {
                                        val newCookies = cookieStorage.get(this.request.url)
                                        request.headers {
                                            set(HttpHeaders.Cookie, renderClientCookies(newCookies))
                                        }
                                        proceed(request.setAuthData(newAutData?.data!!))
                                    } else {
                                        null
                                    }
                                    newCall ?: originalCall
                                }
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

fun HttpClientConfig<*>.installLogger(isDebug: Boolean) {
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

fun HttpClientConfig<*>.installErrorTracker(
    analyticsProvider: () -> Analytics
) {
    install(
        createClientPlugin("Error Tracker") {
            // Took the idea from Logger Plugin
            client.requestPipeline.intercept(HttpRequestPipeline.Before) {
                try {
                    proceed()
                } catch (cause: Throwable) {
                    analyticsProvider().send(
                        AnalyticEvent.createErrorEvent(
                            message = "ErrorHttpRequest_" + context.url.pathSegments.joinToString("/"),
                            throwable =  cause,
                        )
                    )
                    throw cause
                }
            }

            client.responsePipeline.intercept(HttpResponsePipeline.Receive) {
                try {
                   proceed()
                } catch (cause: Throwable) {
                    analyticsProvider().send(
                        AnalyticEvent.createErrorEvent(
                            message = "ErrorHttpResponse_" + context.request.url.pathSegments.joinToString("/"),
                            throwable =  cause,
                        )
                    )
                    throw cause
                }
            }

            client.sendPipeline.intercept(HttpSendPipeline.Before) {
                try {
                    proceed()
                } catch (cause: Throwable) {
                    analyticsProvider().send(
                        AnalyticEvent.createErrorEvent(
                            message = "ErrorHttpSend_" + context.url.pathSegments.joinToString("/"),
                            throwable =  cause,
                        )
                    )
                    throw cause
                }
            }

            client.receivePipeline.intercept(HttpReceivePipeline.Before) { response ->
                try {
                    if (response.status != HttpStatusCode.OK) {
                        val message = "ErrorHttpReceiveStatus_" + response.request.url.pathSegments.joinToString("/") + ":" + response.status.value
                        AnalyticEvent.createErrorEvent(
                            message = message,
                            throwable =  RuntimeException(message),
                        )
                    }
                    proceed()
                } catch (cause: Throwable) {
                    analyticsProvider().send(
                        AnalyticEvent.createErrorEvent(
                            message = "ErrorHttpReceive_" + response.request.url.pathSegments.joinToString("/"),
                            throwable =  cause,
                        )
                    )
                    throw cause
                }
            }
        }
    )
}

private fun renderClientCookies(cookies: List<Cookie>): String =
    cookies.joinToString("; ", transform = ::renderCookieHeader)
