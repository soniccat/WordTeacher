package com.aglushkov.wordteacher.shared.features.add_article.vm

import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.ResourceRepository
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

fun <T> ResourceRepository<T>.toArticleContentExtractor(
    canExtract: (uri: String) -> Boolean,
    transformer: (r: T) -> ArticleContent
): ArticleContentExtractor {
    return object : ArticleContentExtractor  {
        override fun canExtract(uri: String): Boolean {
            return canExtract(uri)
        }

        override fun extract(uri: String): Flow<Resource<ArticleContent>> {
            return this@toArticleContentExtractor.load().map {
                it.transform(loadedDataTransformer = transformer)
            }
        }
    }
}
