package com.aglushkov.wordteacher.shared.repository.article

import com.aglushkov.wordteacher.shared.general.article_parser.ArticleParser
import com.aglushkov.wordteacher.shared.general.article_parser.ParsedArticle
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class ArticleParserRepository: SimpleResourceRepository<ParsedArticle, String>() {
    private val parser = ArticleParser()
    private val httpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 10000  // Total time for an entire HTTP call (5s)
            connectTimeoutMillis = 5000   // Time to establish a connection (5s)
            socketTimeoutMillis = 5000    // Max time of inactivity between data packets (5s)
        }
    }

    override suspend fun loadInternal(arg: String): ParsedArticle {
        val res: HttpResponse = httpClient.get(arg)
        val responseString: String = res.body()
        return parser.parse(responseString)
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
