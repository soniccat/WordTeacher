package com.aglushkov.wordteacher.shared.repository.db

import com.aglushkov.extensions.firstLong
import com.aglushkov.wordteacher.cache.DBArticle
import com.aglushkov.wordteacher.cache.DBNLPSentence
import com.aglushkov.wordteacher.shared.cache.SQLDelightDatabase
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.model.ShortArticle
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppDatabase(driverFactory: DatabaseDriverFactory) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val driver = driverFactory.createDriver()
    private var db = SQLDelightDatabase(driver)

    val articles = Articles()
    val sentencesNLP = DBNLPSentences()

    val state = MutableStateFlow<Resource<AppDatabase>>(Resource.Uninitialized())

    init {
        create()
    }

    suspend fun waitUntilInitialized() = state.first { it.isLoaded() }

    fun create() {
        state.value = Resource.Loading(this@AppDatabase)
        mainScope.launch(Dispatchers.Default) {
            try {
                createDb()
                state.value = Resource.Loaded(this@AppDatabase)
            } catch (e: Exception) {
                state.value = Resource.Error(e, true)
            }
        }
    }

    private fun createDb() {
        SQLDelightDatabase.Schema.create(driver)
    }

    inner class DBNLPSentences {
        fun insert(nlpSentence: NLPSentence) = db.dBNLPSentenceQueries.insert(
            nlpSentence.articleId,
            nlpSentence.orderId,
            nlpSentence.text,
            nlpSentence.tokens.joinToString(nlpSeparator),
            nlpSentence.tags.joinToString(nlpSeparator),
            nlpSentence.lemmas.joinToString(nlpSeparator),
            nlpSentence.chunks.joinToString(nlpSeparator)
        )

        fun selectAll() = db.dBNLPSentenceQueries.selectAll()
        fun selectForArticle(articleId: Long) = db.dBNLPSentenceQueries
            .selectForArticle(articleId)
            .executeAsList()
            .map {
                it.toNLPSentence()
            }

        fun removeAll() = db.dBNLPSentenceQueries.removeAll()
    }

    inner class Articles {
        fun insert(article: Article) = db.dBArticleQueries.insert(article.name, article.date, article.text)
        fun insertedArticleId() = db.dBArticleQueries.lastInsertedRowId().firstLong()
        fun selectAll() = db.dBArticleQueries.selectAll { id, name, date, text ->
            Article(id, name, date, text)
        }
        fun selectAllShortArticles() = db.dBArticleQueries.selectShort { id, name, date ->
            ShortArticle(id, name, date)
        }
        fun selectArticle(anId: Long) = db.dBArticleQueries.selectArticle(anId) { id, name, date, text ->
            val sentences = sentencesNLP.selectForArticle(anId)
            Article(id, name, date, text, sentences)
        }

        fun removeAll() = db.dBArticleQueries.removeAll()
    }

    companion object {
        const val nlpSeparator = "&&"
    }
}

fun DBNLPSentence.toNLPSentence(): NLPSentence {
    val tokens = tokens.split(AppDatabase.nlpSeparator)
    val tags = tags.split(AppDatabase.nlpSeparator)
    val lemmas = lemmas.split(AppDatabase.nlpSeparator)
    val chunks = chunks.split(AppDatabase.nlpSeparator)

    return NLPSentence(
        articleId,
        orderId,
        text,
        tokens.toTypedArray(),
        tags.toTypedArray(),
        lemmas.toTypedArray(),
        chunks.toTypedArray()
    )
}
