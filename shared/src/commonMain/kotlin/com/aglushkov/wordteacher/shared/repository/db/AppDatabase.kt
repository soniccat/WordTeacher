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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppDatabase(driverFactory: DatabaseDriverFactory) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val driver = driverFactory.createDriver()
    private var db = SQLDelightDatabase(driver)

    val articles = Articles()
    val sentencesNLP = DBNLPSentences()
    val cardSets = CardSets()
    val cards = Cards()
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
            nlpSentence.tokenStrings().joinToString(NLP_SEPARATOR),
            nlpSentence.tags.joinToString(NLP_SEPARATOR),
            nlpSentence.lemmas.joinToString(NLP_SEPARATOR),
            nlpSentence.chunks.joinToString(NLP_SEPARATOR)
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
        fun insert(name: String, date: Long, style: ArticleStyle) =
            db.dBArticleQueries.insert(name, date, encodeStyle(style))
        fun insertedArticleId() = db.dBArticleQueries.lastInsertedRowId().firstLong()
//        fun selectAll() = db.dBArticleQueries.selectAll { id, name, date, style ->
//            Article(id,name, date, style = decodeStyle(style))
//        }
        fun selectAllShortArticles() = db.dBArticleQueries.selectShort { id, name, date ->
            ShortArticle(id, name, date)
        }
        fun selectArticle(anId: Long) = db.dBArticleQueries.selectArticle(anId) { id, name, date, style ->
            val sentences = sentencesNLP.selectForArticle(anId)
            Article(id, name, date, sentences, decodeStyle(style))
        }
        fun removeArticle(anId: Long) = db.dBArticleQueries.removeArticle(anId)
        fun removeAll() = db.dBArticleQueries.removeAll()

        private fun decodeStyle(style: String): ArticleStyle =
            Json {
                ignoreUnknownKeys = true
            }.decodeFromString<ArticleStyle>(style)

        private fun encodeStyle(style: ArticleStyle): String =
            Json {
                ignoreUnknownKeys = true
            }.encodeToString(style)
    }

    inner class CardSets {
        fun insert(name: String, date: Long) = db.dBCardSetQueries.insert(name, date)
        fun selectAll() = db.dBCardSetQueries.selectAll(mapper = ::ShortCardSet)
        fun selectCardSet(id: Long) = db.dBCardSetQueries.selectCardSetWithCards(id, mapper = ::CardSet)
        fun removeCardSet(cardSetId: Long) {
            db.transaction {
                db.dBCardQueries.removeCardsBySetId(cardSetId)
                db.dBCardSetToCardRelationQueries.removeByCardSet(cardSetId)
                db.dBCardSetQueries.removeCardSet(cardSetId)
            }
        }
    }

    inner class Cards {
        fun selectCards(setId: Long) = db.dBCardSetToCardRelationQueries.selectCards(
            setId,
            mapper = { id, date, term, partOfSpeech, transcription, definition, synonyms, examples ->
                Card(
                    id!!,
                    date!!,
                    term!!,
                    definition!!,
                    WordTeacherWord.PartOfSpeech.fromString(partOfSpeech!!),
                    transcription,
                    synonyms?.split(CARD_SEPARATOR).orEmpty(),
                    examples?.split(CARD_SEPARATOR).orEmpty()
                )
            }
        )
        fun insertCard(
            setId: Long,
            date: Long,
            term: String,
            definition: String,
            partOfSpeech: WordTeacherWord.PartOfSpeech,
            transcription: String?,
            synonyms: List<String>,
            examples: List<String>
        ) {
            db.transaction {
                db.dBCardQueries.insert(
                    date,
                    term,
                    partOfSpeech.toStringDesc().toString(),
                    transcription,
                    definition,
                    synonyms.joinToString(CARD_SEPARATOR),
                    examples.joinToString(CARD_SEPARATOR)
                )

                val cardId = db.dBCardQueries.lastInsertedRowId().firstLong()!!
                db.dBCardSetToCardRelationQueries.insert(setId, cardId)
            }
        }
        fun updateCard(
            cardId: Long,
            date: Long,
            term: String,
            definition: String,
            partOfSpeech: WordTeacherWord.PartOfSpeech,
            transcription: String?,
            synonyms: List<String>,
            examples: List<String>
        ) = db.dBCardQueries.updateCard(
            date,
            term,
            partOfSpeech.toStringDesc().toString(),
            transcription,
            definition,
            synonyms.joinToString(CARD_SEPARATOR),
            examples.joinToString(CARD_SEPARATOR),
            cardId
        )
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
        const val NLP_SEPARATOR = "&&"
        const val CARD_SEPARATOR = "$$"
    }
}

fun DBNLPSentence.toNLPSentence(): NLPSentence {
    val tokens = tokens.split(AppDatabase.NLP_SEPARATOR)
    val tags = tags.split(AppDatabase.NLP_SEPARATOR)
    val lemmas = lemmas.split(AppDatabase.NLP_SEPARATOR)
    val chunks = chunks.split(AppDatabase.NLP_SEPARATOR)

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