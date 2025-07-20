package com.aglushkov.wordteacher.shared.general

import co.touchlab.stately.concurrency.AtomicLong
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilLoaded
import com.aglushkov.wordteacher.shared.general.resource.Resource
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import kotlin.math.min

// Based on AcceptAllCookiesStorage source code
public class FileCookieStorage(
    private val fileSystem: FileSystem,
    private val path: Path
) : CookiesStorage {

    private val containerState = MutableStateFlow<Resource<MutableList<Cookie>>>(Resource.Uninitialized())
    private val container: MutableList<Cookie> = mutableListOf()
    private val oldestCookie: AtomicLong = AtomicLong(0L)
    private val mutex = Mutex()

    val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        scope.launch(Dispatchers.IO) {
            try {
                val loadedState = load()!!
                oldestCookie.set(loadedState.oldestCookie)
                container.addAll(
                    loadedState.cookies.map { cookieWrapper ->
                        cookieWrapper.toCookie()
                    }
                )
                containerState.value = Resource.Loaded(container)
            } catch (e: Exception) {
                containerState.value = Resource.Loaded(data = mutableListOf())
            }
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        containerState.waitUntilLoaded()
        return mutex.withLock {
            val date = GMTDate()
            if (date.timestamp >= oldestCookie.get()) cleanup(date.timestamp)

            return@withLock container.filter { it.matches(requestUrl) }
        }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        containerState.waitUntilLoaded()
        mutex.withLock {
            with(cookie) {
                if (name.isBlank()) return@withLock
            }

            container.removeAll { it.name == cookie.name && it.matches(requestUrl) }
            container.add(cookie.fillDefaults(requestUrl))
            cookie.expires?.timestamp?.let { expires ->
                if (oldestCookie.get() > expires) {
                    oldestCookie.set(expires)
                }
            }
            saveAsync()
        }
    }

    override fun close() {
    }

    private fun cleanup(timestamp: Long) {
        container.removeAll { cookie ->
            val expires = cookie.expires?.timestamp ?: return@removeAll false
            expires < timestamp
        }

        val newOldest = container.fold(Long.MAX_VALUE) { acc, cookie ->
            cookie.expires?.timestamp?.let { min(acc, it) } ?: acc
        }

        oldestCookie.set(newOldest)
        saveAsync()
    }

    // save / load

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
        }
    }

    private fun saveAsync() {
        scope.launch(Dispatchers.IO) {
            save()
        }
    }

    private fun save() {
        fileSystem.write(path) {
            writeUtf8(VERSION)
            val state = CookieStorageState(
                cookies = container.map {  cookie ->
                    CookieWrapper(
                        name = cookie.name,
                        value = cookie.value,
                        encoding = cookie.encoding,
                        maxAge = cookie.maxAge,
                        expiresTimestamp = cookie.expires?.timestamp,
                        domain = cookie.domain,
                        path = cookie.path,
                        secure = cookie.secure,
                        httpOnly = cookie.httpOnly,
                        extensions = cookie.extensions,
                    )
                },
                oldestCookie = oldestCookie.get()
            )
            writeUtf8("\n")
            writeUtf8(json.encodeToString(state))
        }
    }

    private fun load(): CookieStorageState? {
        return fileSystem.read(path) {
            val ver = readUtf8Line()
            if (ver != VERSION) {
                return@read null
            }

            val stateString = readUtf8()
            return@read json.decodeFromString(stateString)
        }
    }
}

private fun Cookie.matches(requestUrl: Url): Boolean {
    val domain = domain?.toLowerCasePreservingASCIIRules()?.trimStart('.')
        ?: error("Domain field should have the default value")

    val path = with(path) {
        val current = path ?: error("Path field should have the default value")
        if (current.endsWith('/')) current else "$path/"
    }

    val host = requestUrl.host.toLowerCasePreservingASCIIRules()
    val requestPath = let {
        val pathInRequest = requestUrl.encodedPath
        if (pathInRequest.endsWith('/')) pathInRequest else "$pathInRequest/"
    }

    if (host != domain && (hostIsIp(host) || !host.endsWith(".$domain"))) {
        return false
    }

    if (path != "/" &&
        requestPath != path &&
        !requestPath.startsWith(path)
    ) return false

    return !(secure && !requestUrl.protocol.isSecure())
}

private fun Cookie.fillDefaults(requestUrl: Url): Cookie {
    var result = this

    if (result.path?.startsWith("/") != true) {
        result = result.copy(path = requestUrl.encodedPath)
    }

    if (result.domain.isNullOrBlank()) {
        result = result.copy(domain = requestUrl.host)
    }

    return result
}

@Serializable
private data class CookieStorageState(
    val cookies: List<CookieWrapper>,
    val oldestCookie: Long,
)

@Serializable
private data class CookieWrapper(
    val name: String,
    val value: String,
    val encoding: CookieEncoding = CookieEncoding.URI_ENCODING,
    val maxAge: Int = 0,
    val expiresTimestamp: Long? = null,
    val domain: String? = null,
    val path: String? = null,
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val extensions: Map<String, String?> = emptyMap()
) {
    fun toCookie() = Cookie(
        name = name,
        value = value,
        encoding = encoding,
        maxAge = maxAge,
        expires = expiresTimestamp?.let { timestamp ->
            GMTDate(timestamp)
        },
        domain = domain,
        path = path,
        secure = secure,
        httpOnly = httpOnly,
        extensions = extensions,
    )
}

private const val VERSION = "1.0"
