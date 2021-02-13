package com.aglushkov.wordteacher.shared.repository.article

import com.aglushkov.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.toArticle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArticleRepository(
    private val database: AppDatabase,
    private val nlpCore: NLPCore
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<List<Article>>>(Resource.Uninitialized())

    val articles: StateFlow<Resource<List<Article>>> = stateFlow

    init {
        scope.launch(Dispatchers.Default) {
            database.articles.selectAll().asFlow().collect {
                val result = it.executeAsList().map { dbArticle ->
                    val sentences = database.sentencesNLP.selectForArticle(dbArticle.id, nlpCore)
                    dbArticle.toArticle().apply {
                        this.sentences = sentences
                    }
                }
                stateFlow.value = Resource.Loaded(result)
            }
        }
    }

    suspend fun putArticle(article: Article) = coroutineScope {
        nlpCore.waitUntilInitialized()

        withContext(Dispatchers.Default) {
            putArticleInternal(article)
        }
    }

    private fun putArticleInternal(article: Article) {
        val articleId = database.articles.run {
            insert(article)
            insertedArticleId()
        } ?: 0L

        val resultText = clearString(article.text)
        try {
            val nlpCoreCopy = nlpCore.clone()
            nlpCoreCopy.sentences(resultText).forEachIndexed { index, s ->
                val nlpSentence = NLPSentence(nlpCoreCopy, articleId, index.toLong(), s)
                database.sentencesNLP.insert(nlpSentence)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println(resultText)
        }
    }

    // TODO: use string builder
    private fun clearString(s: String) = s.replace(13.toChar(), ' ')
        .replace(10.toChar(), ' ')
        .replace(9.toChar(), ' ')
        .replace(11.toChar(), ' ')
        .replace(13.toChar(), ' ')
        .replace(3.toChar(), ' ')
        .replace("``", "\"")
        .replace("''", "\"")
        .trim()
}