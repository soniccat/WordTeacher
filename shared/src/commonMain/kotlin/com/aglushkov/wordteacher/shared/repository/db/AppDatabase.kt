package com.aglushkov.wordteacher.shared.repository.db

import com.aglushkov.extensions.firstLong
import com.aglushkov.wordteacher.cache.DBCard
import com.aglushkov.wordteacher.cache.DBNLPSentence
import com.aglushkov.wordteacher.shared.cache.SQLDelightDatabase
import com.aglushkov.wordteacher.shared.model.CardProgress
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.model.*
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.TokenSpan
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.TransactionWithoutReturn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull.content

class AppDatabase(driverFactory: DatabaseDriverFactory) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val driver = driverFactory.createDriver()
    private var db = SQLDelightDatabase(
        driver,
        DBCardAdapter = DBCard.Adapter(
            StringListAdapter(),
            StringListAdapter(),
            StringListAdapter(),
        ),
        DBNLPSentenceAdapter = DBNLPSentence.Adapter(
            StringListAdapter(),
            StringListAdapter(),
            StringListAdapter(),
            StringListAdapter(),
        )
    )

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

    fun transaction(
        noEnclosing: Boolean = false,
        body: TransactionWithoutReturn.() -> Unit
    ) = db.transaction(noEnclosing, body)

    inner class DBNLPSentences {
        fun insert(nlpSentence: NLPSentence) = db.dBNLPSentenceQueries.insert(
            nlpSentence.articleId,
            nlpSentence.orderId,
            nlpSentence.text,
            nlpSentence.tokenStrings(),
            nlpSentence.tags,
            nlpSentence.lemmas,
            nlpSentence.chunks
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
                db.dBCardSetToCardRelationQueries.removeCardSet(cardSetId)
                db.dBCardSetQueries.removeCardSet(cardSetId)
            }
        }
    }

    inner class Cards {
        fun selectAllCardIds() = db.dBCardQueries.selectAllCardIds()
        fun selectAllCards() = db.dBCardQueries.selectAllCards(mapper = cardMapper())

        fun selectCards(ids: List<Long>) = db.dBCardQueries.selectCards(
            ids,
            cardMapper()
        )

        fun selectCards(setId: Long) = db.dBCardSetToCardRelationQueries.selectCards(
            setId,
            mapper = cardMapper()
        )

        private fun cardMapper() : (
            id: Long?,
            date: Long?,
            term: String?,
            partOfSpeech: String?,
            transcription: String?,
            definitions: List<String>?,
            synonyms: List<String>?,
            examples: List<String>?,
            progressLevel: Int?,
            progressLastMistakeCount: Int?,
            progressLastLessonDate: Long?
        ) -> Card =
            { id, date, term, partOfSpeech, transcription, definitions, synonyms, examples, progressLevel, progressLastMistakeCount, progressLastLessonDate ->
                Card(
                    id!!,
                    date!!,
                    term!!,
                    definitions.orEmpty(),
                    partOfSpeechEnum(partOfSpeech),
                    transcription,
                    synonyms.orEmpty(),
                    examples.orEmpty(),
                    progress = CardProgress(
                        progressLevel ?: 0,
                        progressLastMistakeCount ?: 0,
                        progressLastLessonDate ?: 0L
                    )
                )
            }

        fun insertCard(
            setId: Long,
            term: String = "",
            date: Long = 0,
            definitions: List<String> = listOf(),
            partOfSpeech: WordTeacherWord.PartOfSpeech = WordTeacherWord.PartOfSpeech.Undefined,
            transcription: String? = "",
            synonyms: List<String> = mutableListOf(),
            examples: List<String> = mutableListOf(),
            progress: CardProgress = CardProgress.EMPTY
        ): Card {
            var newCard = Card(
                id = -1,
                date = date,
                term = term,
                definitions = definitions,
                partOfSpeech = partOfSpeech,
                transcription = transcription,
                synonyms = synonyms,
                examples = examples,
                progress = progress
            )

            cards.insertCardInternal(setId, newCard)
            newCard = newCard.copy(id = cards.insertedCardId()!!)
            return newCard
        }

        private fun insertCardInternal(setId: Long, newCard: Card) {
            cards.insertCardInternal(
                setId = setId,
                date = newCard.date,
                term = newCard.term,
                definitions = newCard.definitions,
                partOfSpeech = newCard.partOfSpeech,
                transcription = newCard.transcription,
                synonyms = newCard.synonyms,
                examples = newCard.examples,
                progress = newCard.progress
            )
        }

        private fun insertCardInternal(
            setId: Long,
            date: Long,
            term: String,
            definitions: List<String>,
            partOfSpeech: WordTeacherWord.PartOfSpeech,
            transcription: String?,
            synonyms: List<String>,
            examples: List<String>,
            progress: CardProgress
        ) {
            db.transaction {
                db.dBCardQueries.insert(
                    date,
                    term,
                    partOfSpeech.toString(),
                    transcription,
                    definitions,
                    synonyms,
                    examples,
                    progress.currentLevel,
                    progress.lastMistakeCount,
                    progress.lastLessonDate
                )

                val cardId = db.dBCardQueries.lastInsertedRowId().firstLong()!!
                db.dBCardSetToCardRelationQueries.insert(setId, cardId)
            }
        }

        fun updateCard(
            card: Card
        ): Card {
            updateCard(
                cardId = card.id,
                date = card.date,
                term = card.term,
                definitions = card.definitions,
                partOfSpeech = card.partOfSpeech,
                transcription = card.transcription,
                synonyms = card.synonyms,
                examples = card.examples,
                progressLevel = card.progress.currentLevel,
                progressLastMistakeCount = card.progress.lastMistakeCount,
                progressLastLessonDate = card.progress.lastLessonDate,
            )
            return card
        }

        fun updateCard(
            cardId: Long,
            date: Long,
            term: String,
            definitions: List<String>,
            partOfSpeech: WordTeacherWord.PartOfSpeech,
            transcription: String?,
            synonyms: List<String>,
            examples: List<String>,
            progressLevel: Int,
            progressLastMistakeCount: Int,
            progressLastLessonDate: Long
        ) = db.dBCardQueries.updateCard(
            date,
            term,
            partOfSpeech.toString(),
            transcription,
            definitions,
            synonyms,
            examples,
            progressLevel,
            progressLastMistakeCount,
            progressLastLessonDate,
            cardId
        )

        fun insertedCardId() = db.dBCardSetQueries.lastInsertedRowId().firstLong()

        fun removeCard(cardId: Long) {
            db.transaction {
                db.dBCardSetToCardRelationQueries.removeCard(cardId)
                db.dBCardQueries.removeCard(cardId)
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
}

fun DBNLPSentence.toNLPSentence(): NLPSentence {
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

// TODO: write tests
private class StringListAdapter: ColumnAdapter<List<String>, String> {

    override fun decode(databaseValue: String): List<String> {
        val resultList = mutableListOf<String>()

        var charIndex = 0
        val len = databaseValue.length
        val value = StringBuilder()

        while (charIndex < len) {
            var ch = databaseValue[charIndex]
            if (ch == LIST_DIVIDER) {
                resultList.add(value.toString())
                value.clear()

                ++charIndex
                continue

            } else if (ch == LIST_ESCAPE) {
                if (charIndex + 1 < len) {
                    val nextCh = databaseValue[charIndex + 1]
                    if (nextCh == LIST_ESCAPE) {
                        ch = nextCh
                        ++charIndex
                    } else if (nextCh == LIST_DIVIDER) {
                        ch = nextCh
                        ++charIndex
                    }
                }
            }

            value.append(ch)
            ++charIndex
        }

        if (databaseValue.isNotEmpty()) {
            resultList.add(value.toString())
        }

        return resultList
    }

    override fun encode(value: List<String>): String {
        return value.joinToString(LIST_DIVIDER.toString()) { str ->
            val sb = StringBuilder(str)
            var charIndex = 0
            var len = sb.length

            while (charIndex < len) {
                val ch = sb[charIndex]
                if (ch == LIST_DIVIDER) {
                    sb.insert(charIndex, LIST_ESCAPE)
                    ++charIndex
                    ++len
                } else if (ch == LIST_ESCAPE) {
                    sb.insert(charIndex, LIST_ESCAPE)
                    ++charIndex
                    ++len
                }

                ++charIndex
            }
            sb
        }
    }
}

private const val LIST_DIVIDER = '|'
private const val LIST_ESCAPE = '\\'
