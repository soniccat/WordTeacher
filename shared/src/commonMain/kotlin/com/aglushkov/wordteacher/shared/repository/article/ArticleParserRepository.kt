package com.aglushkov.wordteacher.shared.repository.article

import com.aglushkov.wordteacher.shared.general.article_parser.ArticleParser
import com.aglushkov.wordteacher.shared.general.article_parser.ParsedArticle
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArticleParserRepository: SimpleResourceRepository<ParsedArticle, String>() {
    private val parser = ArticleParser()
    private val httpClient = HttpClient {
    }

    override suspend fun load(arg: String): ParsedArticle = withContext(Dispatchers.Default) {
        val res: HttpResponse = httpClient.get(arg)
        val responseString = res.readBytes().decodeToString()
        parser.parse(responseString)
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
