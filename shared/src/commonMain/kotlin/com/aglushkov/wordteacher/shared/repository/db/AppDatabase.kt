package com.aglushkov.wordteacher.shared.repository.db

import com.aglushkov.extensions.firstLong
import com.aglushkov.wordteacher.cache.DBNLPSentence
import com.aglushkov.wordteacher.shared.cache.SQLDelightDatabase
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.model.*
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.TokenSpan
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
    val cardSets = CardSets()
    val notes = Notes()

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
            nlpSentence.tokenStrings().joinToString(nlpSeparator),
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
        // TODO: looks strange that we pass a fake article to create an article,
        //  replace with separate arguments (name, date, text)
        fun insert(article: Article) = db.dBArticleQueries.insert(article.name, article.date, article.text)
        fun insertedArticleId() = db.dBArticleQueries.lastInsertedRowId().firstLong()
        fun selectAll() = db.dBArticleQueries.selectAll(mapper = ::Article)
        fun selectAllShortArticles() = db.dBArticleQueries.selectShort { id, name, date ->
            ShortArticle(id, name, date)
        }
        fun selectArticle(anId: Long) = db.dBArticleQueries.selectArticle(anId) { id, name, date, text ->
            val sentences = sentencesNLP.selectForArticle(anId)
            Article(id, name, date, text, sentences)
        }

        fun removeArticle(anId: Long) = db.dBArticleQueries.removeArticle(anId)
        fun removeAll() = db.dBArticleQueries.removeAll()
    }

    inner class CardSets {
        fun insert(name: String, date: Long) = db.dBCardSetQueries.insert(name, date)
        fun selectAll() = db.dBCardSetQueries.selectAll(mapper = ::ShortCardSet)
    }

    inner class Cards {
        fun insertCard(setId: Long, card: Card) {
            db.transaction {
                db.dBCardQueries.insert(card.date, card.term, card.definition)

                val cardId = db.dBCardQueries.lastInsertedRowId().firstLong()!!
                db.dBCardSetToCardRelationQueries.insert(setId, cardId)
            }
        }
    }

    inner class Notes {
        fun insert(date: Long, text: String) = db.dBNoteQueries.insert(date, text)
        fun insertedNoteId() = db.dBNoteQueries.lastInsertedRowId().firstLong()
        fun selectAll() = db.dBNoteQueries.selectAll(mapper = ::Note)
        fun removeNote(id: Long) = db.dBNoteQueries.removeNote(id)
        fun removeAll() = db.dBNoteQueries.removeAll()
        fun updateNote(id: Long, text: String) = db.dBNoteQueries.update(text, id)
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
        tokensToTokenSpans(text, tokens),
        tags,
        lemmas,
        chunks
    )
}

private fun tokensToTokenSpans(text: String, tokenStrings: List<String>): List<TokenSpan> {
    if (tokenStrings.isEmpty()) return emptyList()

    val resultTokens = mutableListOf<TokenSpan>()
    var i = 0
    var tokenIndex = 0
    var currentToken = tokenStrings.firstOrNull()

    while (currentToken != null && i < text.length) {
        if (text.subSequence(i, i + currentToken.length) == currentToken) {
            resultTokens.add(TokenSpan(i, i + currentToken.length))
            i += currentToken.length
            ++tokenIndex

            currentToken = if (tokenIndex < tokenStrings.size) {
                tokenStrings[tokenIndex]
            } else {
                null
            }
        } else {
            ++i
        }
    }

    return resultTokens
}