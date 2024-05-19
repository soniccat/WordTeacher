package com.aglushkov.wordteacher.shared.features.add_article.vm

import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.ResourceRepository
import com.aglushkov.wordteacher.shared.repository.article.ArticleParserRepository
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ArticleContent(
    var title: String? = null,
    var text: String? = null,
)

interface ArticleContentExtractor {
    fun canExtract(uri: String): Boolean
    fun extract(uri: String): Flow<Resource<ArticleContent>>
}

fun <T> ResourceRepository<T, String>.toArticleContentExtractor(
    canExtract: (uri: String) -> Boolean,
    transformer: (r: T) -> ArticleContent
): ArticleContentExtractor {
    return object : ArticleContentExtractor  {
        override fun canExtract(uri: String): Boolean {
            return canExtract(uri)
        }

        override fun extract(uri: String): Flow<Resource<ArticleContent>> {
            return this@toArticleContentExtractor.load(uri).map {
                it.map(loadedDataTransformer = transformer)
            }
        }
    }
}

fun ArticleParserRepository.toArticleContentExtractor(): ArticleContentExtractor {
    return this.toArticleContentExtractor(
        canExtract = {
            try {
                val protocol = Url(it).protocol
                protocol == URLProtocol.HTTP || protocol == URLProtocol.HTTPS
            } catch (t: Exception) {
                false
            }
        },
        transformer = { article ->
            ArticleContent(
                title = article.title,
                text = article.text
            )
        }
    )
}
