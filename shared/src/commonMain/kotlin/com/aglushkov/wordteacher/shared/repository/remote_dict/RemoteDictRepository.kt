package com.aglushkov.wordteacher.shared.repository.remote_dict

import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDict
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.general.okio.deleteIfExists
import com.aglushkov.wordteacher.shared.general.okio.withProgress
import com.aglushkov.wordteacher.shared.general.okio.writeTo
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.asLoaded
import com.aglushkov.wordteacher.shared.general.resource.asLoading
import com.aglushkov.wordteacher.shared.general.resource.isError
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.loadResourceWithProgress
import com.aglushkov.wordteacher.shared.general.toOkResponse
import io.ktor.client.*
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Url
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.asSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.io.okio.asOkioSource
import okio.Path
import okio.Sink
import okio.buffer
import okio.sink
import java.net.URL

class RemoteDictRepository(
    private val dictService: WordTeacherDictService,
    private val loadingDictPath: Path,
): SimpleResourceRepository<List<WordTeacherDict>, Unit>() {

    private val loadingProgress: MutableStateFlow<Map<WordTeacherDict, Resource<Unit>>> =
        MutableStateFlow(mapOf())
    val remoteDictWithProgress = combine(
        stateFlow,
        loadingProgress
    ) { dictsRes, progress ->
        dictsRes.mapLoadedData { dicts ->
            dicts.map { it to (progress[it]?.asLoading()?.progress ?: -1f) }
        }
    }

    override suspend fun loadInternal(arg: Unit): List<WordTeacherDict> {
        return dictService.files().toOkResponse().let {
            it.dicts.orEmpty()
        }
    }

    suspend fun loadDict(dict: WordTeacherDict): Flow<Resource<Unit>> {
        return obtainLoadingProgress(dict)
    }

    fun loadedDictPath(dict: WordTeacherDict): Path {
        val url = Url(dict.url)
        return loadingDictPath.div(url.segments.last())
    }

    private suspend fun obtainLoadingProgress(
        dict: WordTeacherDict
    ): Flow<Resource<Unit>> {
        val dictLoadingRes = loadingProgress.value[dict]
        if (dictLoadingRes != null) {
            return loadingProgress.map {
                it[dict] ?: Resource.Uninitialized()
            }
        }

        val loadedPath = loadedDictPath(dict)
        loadedPath.deleteIfExists()

        loadingProgress.update {
            it + (dict to Resource.Loading(Unit))
        }
        loadResourceWithProgress(
            loader = downloadFileFromUrl(
                remoteUrl = dict.url,
                destination = loadedPath.toFile().sink(),
            ).map { 
                it to Unit
            }
        ).collect { dictProgress ->
            loadingProgress.update { map ->
                map.mapValues { entry ->
                    if (entry.key == dict) {
                        dictProgress
                    } else {
                        entry.value
                    }
                }
            }

            if (dictProgress.isError()) {
                loadedPath.deleteIfExists()
            }
        }

        return loadingProgress.map {
            it[dict] ?: Resource.Uninitialized()
        }
    }

    val client = HttpClient {
        engine {
            followRedirects = true
        }
    }

    suspend fun downloadFileFromUrl(
        remoteUrl: String,
        destination: Sink
    ): Flow<Float> = channelFlow {
        client.prepareGet(remoteUrl).execute { response ->
            if (!response.status.isSuccess()) {
                error("Server returned HTTP error: ${response.status}")
            }

            val contentLength = response.contentLength() ?: 0L
            response.bodyAsChannel()
                .asSource()
                .asOkioSource()
                .withProgress { bytesRead, isDone ->
                    trySend(
                        if (contentLength == 0L) {
                            0.0f
                        } else {
                            bytesRead.toFloat() / contentLength.toFloat()
                        }
                    )
                }.buffer()
                .writeTo(destination)
        }
    }
}
