package com.aglushkov.wordteacher.shared.repository.article

import com.aglushkov.wordteacher.shared.general.article_parser.ArticleParser
import com.aglushkov.wordteacher.shared.general.article_parser.ParsedArticle
import com.aglushkov.wordteacher.shared.general.ktor.CustomHeader
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isNotLoadedAndNotLoading
import com.aglushkov.wordteacher.shared.general.resource.tryInResource
import com.aglushkov.wordteacher.shared.model.Article
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ArticleParserRepository {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<ParsedArticle>>(Resource.Uninitialized())

    val parsedArticle: StateFlow<Resource<ParsedArticle>> = stateFlow

    private val parser = ArticleParser()

    private val httpClient = HttpClient {
    }

    // TODO: refactor, simplify loading, version setting / bumping
    suspend fun parse(url: String): Resource<ParsedArticle> {
        val currentValue = stateFlow.value
        val needLoad = currentValue.isNotLoadedAndNotLoading()

        // Keep version for Uninitialized to support flow collecting in advance when services aren't loaded
        val nextVersion = if (needLoad && !currentValue.isUninitialized()) {
            currentValue.version + 1
        } else {
            currentValue.version
        }

        stateFlow.value = Resource.Loading(version = nextVersion)

        try {
            val parsedArticle = withContext(Dispatchers.Default) {
                val res: HttpResponse = httpClient.get(url)
                val responseString = res.readBytes().decodeToString()
                parser.parse(responseString)
            }

            if (stateFlow.value.version == nextVersion ) {
                stateFlow.value = Resource.Loaded(parsedArticle, version = nextVersion)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (stateFlow.value.version == nextVersion ) {
                stateFlow.value = Resource.Error(e, canTryAgain = true, version = nextVersion)
            }
        }

        return stateFlow.value
    }

    suspend fun requestLargerArticle() {
        val parsedArticle = withContext(Dispatchers.Default) {
            parser.largerArticle()
        }

        stateFlow.value = stateFlow.value.bumpVersion().toLoaded(parsedArticle)
    }

    suspend fun requestSmallerArticle() {
        val parsedArticle = withContext(Dispatchers.Default) {
            parser.smallerArticle()
        }

        stateFlow.value = stateFlow.value.bumpVersion().toLoaded(parsedArticle)
    }
}
