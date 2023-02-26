package com.aglushkov.wordteacher.shared.repository.db

import com.aglushkov.extensions.asFlow
import com.aglushkov.extensions.firstLong
import com.aglushkov.wordteacher.cache.DBCard
import com.aglushkov.wordteacher.cache.DBNLPSentence
import com.aglushkov.wordteacher.shared.cache.SQLDelightDatabase
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.model.CardProgress
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.merge
import com.aglushkov.wordteacher.shared.general.resource.tryInResource
import com.aglushkov.wordteacher.shared.general.toLong
import com.aglushkov.wordteacher.shared.model.*
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.TokenSpan
import com.benasher44.uuid.uuid4
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.TransactionWithoutReturn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppDatabase(
    driverFactory: DatabaseDriverFactory,
    private val timeSource: TimeSource
) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val driver = driverFactory.createDriver()
    private var db = SQLDelightDatabase(
        driver,
        DBCardAdapter = DBCard.Adapter(
            StringListAdapter(),
            StringListAdapter(),
            StringListAdapter(),
            SpanListAdapter(),
            SpanListAdapter(),
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

    private val json = Json {
        ignoreUnknownKeys = true
    }

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
        fun removeArticle(anId: Long) {
            db.transaction {
                db.dBNLPSentenceQueries.removeWithArticleId(anId)
                db.dBArticleQueries.removeArticle(anId)
            }
        }
        fun removeAll() = db.dBArticleQueries.removeAll()

        private fun decodeStyle(style: String): ArticleStyle =
            json.decodeFromString<ArticleStyle>(style)

        private fun encodeStyle(style: ArticleStyle): String =
            json.encodeToString(style)
    }

    inner class CardSets {
        fun insert(name: String, date: Long) = db.dBCardSetQueries.insert(name, date, date, uuid4().toString())

        private fun insertedCardSetId() = db.dBCardSetQueries.lastInsertedRowId().firstLong()

        fun selectShortCardSets(): List<ShortCardSet> {
            return db.dBCardSetQueries.selectAll(mapper = { id, name, date, modificationDate, creationId, remoteId ->
                ShortCardSet(id, name, Instant.fromEpochMilliseconds(date), Instant.fromEpochMilliseconds(modificationDate), 0f, 0f, creationId, remoteId)
            }).executeAsList()
        }

        fun selectUpdatedCardSets(afterDate: Long): List<CardSet> = db.dBCardSetQueries.selectUpdated(afterDate, mapper = { id, name, date, modificationDate, creationId, remoteId ->
            CardSet(id, remoteId, name, Instant.fromEpochMilliseconds(date), Instant.fromEpochMilliseconds(modificationDate), emptyList(), creationId)
        }).executeAsList()

        fun selectWithoutRemoteId(): List<CardSet> = db.dBCardSetQueries.selectWithouRemoteId(mapper = { id, name, date, modificationDate, creationId, remoteId ->
            CardSet(id, remoteId, name, Instant.fromEpochMilliseconds(date), Instant.fromEpochMilliseconds(modificationDate), emptyList(), creationId)
        }).executeAsList()

        fun selectAll(): Flow<Resource<List<ShortCardSet>>> {
            // TODO: reorganize db to pull progress from it instead of loading all the cards
            val shortCardSetsFlow = db.dBCardSetQueries.selectAll(mapper = { id, name, date, modificationDate, creationId, remoteId ->
                ShortCardSet(id, name, Instant.fromEpochMilliseconds(date), Instant.fromEpochMilliseconds(modificationDate), 0f, 0f, creationId, remoteId)
            }).asFlow()
            val setsWithCardsFlow = selectAllSetIdsWithCards().asFlow()

            return combine(
                flow = shortCardSetsFlow.map {
                    tryInResource(canTryAgain = true) { it.executeAsList() }
                },
                flow2 = setsWithCardsFlow.map { query ->
                    tryInResource(canTryAgain = true) {
                        query.executeAsList().groupBy({ it.first }, { it.second })
                    }
                },
                transform = { shortCardSetsRes, setsWithCardsRes ->
                    shortCardSetsRes.merge(setsWithCardsRes) { shortSets, setsWithCards ->
                        shortSets.orEmpty().map { set ->
                            val cards = setsWithCards?.get(set.id)
                            set.copy(
                                readyToLearnProgress = cards?.readyToLearnProgress(timeSource) ?: 0f,
                                totalProgress = cards?.totalProgress() ?: 0f
                            )
                        }
                    }
                }
            )
        }

        fun selectCardSet(id: Long) = db.dBCardSetQueries.selectCardSet(id) { id, name, date, modificationDate, creationId, remoteId ->
            CardSet(id, remoteId, name, Instant.fromEpochMilliseconds(date), Instant.fromEpochMilliseconds(modificationDate), emptyList(), creationId)
        }

        fun removeCardSet(cardSetId: Long) {
            db.transaction {
                db.dBCardQueries.removeCardsBySetId(cardSetId)
                db.dBCardSetToCardRelationQueries.removeCardSet(cardSetId)
                db.dBCardSetQueries.removeCardSet(cardSetId)
            }
        }

        fun selectAllSetIdsWithCards() = db.dBCardSetToCardRelationQueries.selectAllSetIdsWithCards { setId, id, date, term, partOfSpeech, transcription, definitions, synonyms, examples, progressLevel, progressLastMistakeCount, progressLastLessonDate, definitionTermSpans, exampleTermSpans, editDate, spanUpdateDate, modificationDate, creationId, remoteId ->
            setId to cards.cardMapper().invoke(id, date, term, partOfSpeech, transcription, definitions, synonyms, examples, progressLevel, progressLastMistakeCount, progressLastLessonDate, definitionTermSpans, exampleTermSpans, editDate, spanUpdateDate, modificationDate, creationId, remoteId)
        }

        fun selectSetIdsWithCards(setIds: List<Long>) = db.dBCardSetToCardRelationQueries.selectSetIdsWithCards(setIds) { setId, id, date, term, partOfSpeech, transcription, definitions, synonyms, examples, progressLevel, progressLastMistakeCount, progressLastLessonDate, definitionTermSpans, exampleTermSpans, editDate, spanUpdateDate, modificationDate, creationId, remoteId ->
            setId to cards.cardMapper().invoke(id, date, term, partOfSpeech, transcription, definitions, synonyms, examples, progressLevel, progressLastMistakeCount, progressLastLessonDate, definitionTermSpans, exampleTermSpans, editDate, spanUpdateDate, modificationDate, creationId, remoteId)
        }

        fun lastModificationDate() = db.dBCardSetQueries.lastModificationDate().executeAsList().firstOrNull()?.MAX ?: 0L

        fun remoteIds() = db.dBCardSetQueries.selectRemoteIds().executeAsList()

        fun insert(cardSet: CardSet): CardSet {
            db.transaction {
                db.dBCardSetQueries.insert(
                    name = cardSet.name,
                    date = cardSet.creationDate.toEpochMilliseconds(),
                    modificationDate = cardSet.modificationDate.toEpochMilliseconds(),
                    creationId = cardSet.creationId
                )
                cardSet.cards.onEach { card ->
                    cards.insertCard(cardSet.requireId(), card)
                }
            }

            return cardSet.copy(
                id = insertedCardSetId()!!
            )
        }

        fun updateCardSet(
            cardSet: CardSet
        ): CardSet {
            db.transaction {
                updateCardSet(
                    id = cardSet.requireId(),
                    name = cardSet.name,
                    date = cardSet.creationDate.toEpochMilliseconds(),
                    modificationDate = cardSet.modificationDate.toEpochMilliseconds(),
                    creationId = cardSet.creationId,
                    remoteId = cardSet.creationId,
                )

                val currentCards = cards.selectCards(cardSet.requireId()).executeAsList()
                val currentCardSet = currentCards.toMutableSet()
                val intersect = cardSet.cards.intersect(currentCardSet)

                // skip intersected cards
                // currentCards - intersect -> old cards to delete
                // cardSet.cards - intersect -> new cards to insert

                val cardIdsToDelete = (currentCards - intersect).map { it.id }
                if (cardIdsToDelete.isNotEmpty()) {
                    db.dBCardSetToCardRelationQueries.removeCards(cardIdsToDelete)
                    db.dBCardQueries.removeCards(cardIdsToDelete)
                }

                (cardSet.cards - intersect).onEach { card ->
                    cards.insertCard(cardSet.requireId(), card)
                }
            }

            return cardSet
        }

        private fun updateCardSet(
            id: Long,
            name: String,
            date: Long,
            modificationDate: Long,
            creationId: String,
            remoteId: String,
        ) = db.dBCardSetQueries.updateCardSet(
            name = name,
            date = date,
            modificationDate = modificationDate,
            creationId = creationId,
            remoteId = remoteId,
            id = id,
        )

        fun updateCardSetRemoteId(remoteId: String, modificationDate: Long, creationId: String) =
            db.dBCardSetQueries.updateCardSetRemoteId(remoteId, modificationDate, creationId)
    }

    inner class Cards {
        fun selectAllCardIds() = db.dBCardQueries.selectAllCardIds()
        fun selectAllCards() = db.dBCardQueries.selectAllCards(mapper = cardMapper())
        fun selectCardsWithOutdatedSpans() =
            db.dBCardQueries.selectCardsWithOutdatedSpans(mapper = cardMapper())

        fun selectCards(ids: List<Long>) = db.dBCardQueries.selectCards(
            ids,
            cardMapper()
        )

        fun selectCards(setId: Long) = db.dBCardSetToCardRelationQueries.selectCards(
            setId,
            mapper = cardMapper()
        )

        fun cardMapper(): (
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
            progressLastLessonDate: Long?,
            definitionTermSpans: List<List<Pair<Int, Int>>>?,
            exampleTermSpans: List<List<Pair<Int, Int>>>?,
            needToUpdateDefinitionSpans: Long?,
            needToUpdateExampleSpans: Long?,
            modificationDate: Long?,
            creationId: String?,
            remoteId: String?
        ) -> Card =
            { id, date, term, partOfSpeech, transcription, definitions, synonyms, examples, progressLevel, progressLastMistakeCount, progressLastLessonDate, definitionTermSpans, exampleTermSpans, needToUpdateDefinitionSpans, needToUpdateExampleSpans, modificationDate, creationId, remoteId ->
                Card(
                    id!!,
                    remoteId.orEmpty(),
                    Instant.fromEpochMilliseconds(date!!),
                    Instant.fromEpochMilliseconds(modificationDate!!),
                    term!!,
                    definitions.orEmpty(),
                    definitionTermSpans.orEmpty(),
                    partOfSpeechEnum(partOfSpeech),
                    transcription,
                    synonyms.orEmpty(),
                    examples.orEmpty(),
                    exampleTermSpans.orEmpty(),
                    progress = CardProgress(
                        progressLevel ?: 0,
                        progressLastMistakeCount ?: 0,
                        progressLastLessonDate ?: 0L
                    ),
                    needToUpdateDefinitionSpans = needToUpdateDefinitionSpans!! != 0L,
                    needToUpdateExampleSpans = needToUpdateExampleSpans!! != 0L,
                    creationId = creationId!!
                )
            }

        fun insertCard(
            setId: Long,
            term: String = "",
            creationDate: Instant = Instant.fromEpochMilliseconds(0),
            modificationDate: Instant = creationDate,
            definitions: List<String> = listOf(),
            definitionTermSpans: List<List<Pair<Int, Int>>> = listOf(),
            partOfSpeech: WordTeacherWord.PartOfSpeech = WordTeacherWord.PartOfSpeech.Undefined,
            transcription: String? = "",
            synonyms: List<String> = mutableListOf(),
            examples: List<String> = mutableListOf(),
            exampleTermSpans: List<List<Pair<Int, Int>>> = listOf(),
            progress: CardProgress = CardProgress.EMPTY,
            needToUpdateDefinitionSpans: Boolean = false,
            needToUpdateExampleSpans: Boolean = false,
            creationId: String = uuid4().toString()
        ): Card {
            var newCard = Card(
                id = -1,
                remoteId = "",
                creationDate = creationDate,
                modificationDate = modificationDate,
                term = term,
                definitions = definitions,
                definitionTermSpans = definitionTermSpans,
                partOfSpeech = partOfSpeech,
                transcription = transcription,
                synonyms = synonyms,
                examples = examples,
                exampleTermSpans = exampleTermSpans,
                progress = progress,
                needToUpdateDefinitionSpans = needToUpdateDefinitionSpans,
                needToUpdateExampleSpans = needToUpdateExampleSpans,
                creationId = creationId,
            )

            cards.insertCard(setId, newCard)
            newCard = newCard.copy(id = cards.insertedCardId()!!)
            return newCard
        }

        fun insertCard(setId: Long, newCard: Card) {
            cards.insertCardInternal(
                setId = setId,
                creationDate = newCard.creationDate.toEpochMilliseconds(),
                modificationDate = newCard.modificationDate.toEpochMilliseconds(),
                term = newCard.term,
                definitions = newCard.definitions,
                definitionTermSpans = newCard.definitionTermSpans,
                partOfSpeech = newCard.partOfSpeech,
                transcription = newCard.transcription,
                synonyms = newCard.synonyms,
                examples = newCard.examples,
                exampleTermSpans = newCard.exampleTermSpans,
                progress = newCard.progress,
                needToUpdateDefinitionSpans = newCard.needToUpdateDefinitionSpans,
                needToUpdateExampleSpans = newCard.needToUpdateExampleSpans,
                creationId = newCard.creationId
            )
        }

        private fun insertCardInternal(
            setId: Long,
            creationDate: Long,
            modificationDate: Long,
            term: String,
            definitions: List<String>,
            definitionTermSpans: List<List<Pair<Int, Int>>> = listOf(),
            partOfSpeech: WordTeacherWord.PartOfSpeech,
            transcription: String?,
            synonyms: List<String>,
            examples: List<String>,
            exampleTermSpans: List<List<Pair<Int, Int>>> = listOf(),
            progress: CardProgress,
            needToUpdateDefinitionSpans: Boolean,
            needToUpdateExampleSpans: Boolean,
            creationId: String,
        ) {
            db.transaction {
                db.dBCardQueries.insert(
                    creationDate,
                    term,
                    partOfSpeech.toString(),
                    transcription,
                    definitions,
                    synonyms,
                    examples,
                    progress.currentLevel,
                    progress.lastMistakeCount,
                    progress.lastLessonDate,
                    definitionTermSpans,
                    exampleTermSpans,
                    needToUpdateDefinitionSpans.toLong(),
                    needToUpdateExampleSpans.toLong(),
                    modificationDate,
                    creationId
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
                remoteId = card.remoteId,
                creationDate = card.creationDate.toEpochMilliseconds(),
                modificationDate = card.modificationDate.toEpochMilliseconds(),
                term = card.term,
                definitions = card.definitions,
                definitionTermSpans = card.definitionTermSpans,
                partOfSpeech = card.partOfSpeech,
                transcription = card.transcription,
                synonyms = card.synonyms,
                examples = card.examples,
                exampleTermSpans = card.exampleTermSpans,
                progressLevel = card.progress.currentLevel,
                progressLastMistakeCount = card.progress.lastMistakeCount,
                progressLastLessonDate = card.progress.lastLessonDate,
                needToUpdateDefinitionSpans = card.needToUpdateDefinitionSpans.toLong(),
                needToUpdateExampleSpans = card.needToUpdateExampleSpans.toLong(),
            )
            return card
        }

        private fun updateCard(
            cardId: Long,
            remoteId: String,
            creationDate: Long,
            modificationDate: Long,
            term: String,
            definitions: List<String>,
            definitionTermSpans: List<List<Pair<Int, Int>>>,
            partOfSpeech: WordTeacherWord.PartOfSpeech,
            transcription: String?,
            synonyms: List<String>,
            examples: List<String>,
            exampleTermSpans: List<List<Pair<Int, Int>>>,
            progressLevel: Int,
            progressLastMistakeCount: Int,
            progressLastLessonDate: Long,
            needToUpdateDefinitionSpans: Long,
            needToUpdateExampleSpans: Long,
        ) {
            db.transaction {
                db.dBCardSetQueries.selectCardIdSetByCardId(cardId).executeAsOneOrNull()?.let {

                }

                db.dBCardQueries.updateCard(
                    creationDate,
                    term,
                    partOfSpeech.toString(),
                    transcription,
                    definitions,
                    synonyms,
                    examples,
                    progressLevel,
                    progressLastMistakeCount,
                    progressLastLessonDate,
                    definitionTermSpans,
                    exampleTermSpans,
                    needToUpdateDefinitionSpans,
                    needToUpdateExampleSpans,
                    modificationDate,
                    remoteId,
                    cardId
                )

            }
        }

        private fun insertedCardId() = db.dBCardSetQueries.lastInsertedRowId().firstLong()

        fun removeCard(cardId: Long) {
            db.transaction {
                db.dBCardSetToCardRelationQueries.removeCard(cardId)
                db.dBCardQueries.removeCard(cardId)
            }
        }

        fun updateCardSetRemoteId(remoteId: String, modificationDate: Long, creationId: String) =
            db.dBCardQueries.updateCardRemoteId(remoteId, modificationDate, creationId)
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
private class StringListAdapter(
    private val divider: Char = LIST_DIVIDER,
    private val escape: Char = LIST_ESCAPE
): ColumnAdapter<List<String>, String> {

    override fun decode(databaseValue: String): List<String> {
        val resultList = mutableListOf<String>()

        var charIndex = 0
        val len = databaseValue.length
        val value = StringBuilder()

        while (charIndex < len) {
            var ch = databaseValue[charIndex]
            if (ch == divider) {
                resultList.add(value.toString())
                value.clear()

                ++charIndex
                continue

            } else if (ch == escape) {
                if (charIndex + 1 < len) {
                    val nextCh = databaseValue[charIndex + 1]
                    if (nextCh == escape) {
                        ch = nextCh
                        ++charIndex
                    } else if (nextCh == divider) {
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
        return value.joinToString(divider.toString()) { str ->
            val sb = StringBuilder(str)
            var charIndex = 0
            var len = sb.length

            while (charIndex < len) {
                val ch = sb[charIndex]
                if (ch == divider) {
                    sb.insert(charIndex, escape)
                    ++charIndex
                    ++len
                } else if (ch == escape) {
                    sb.insert(charIndex, escape)
                    ++charIndex
                    ++len
                }

                ++charIndex
            }
            sb
        }
    }
}

private class SpanListAdapter: ColumnAdapter<List<List<Pair<Int, Int>>>, String> {
    private val outerAdapter = StringListAdapter()
    private val innerAdapter = StringListAdapter(
        divider = '#',
        escape = '$'
    )

    override fun decode(databaseValue: String): List<List<Pair<Int, Int>>> =
        outerAdapter.decode(databaseValue).map { sentence ->
            innerAdapter.decode(sentence).map { stringSpan ->
                stringSpan.decodePair()
            }
        }

    override fun encode(value: List<List<Pair<Int, Int>>>): String =
        outerAdapter.encode(
            value.map { listOfSpans ->
                innerAdapter.encode(
                    listOfSpans.map { it.encode() }
                )
            }
        )
}

private fun Pair<Int,Int>.encode(): String =
    "$first$PAIR_DIVIDER$second"

private fun String.decodePair(): Pair<Int, Int> {
    val numbers = split(PAIR_DIVIDER)
    return Pair(
        first = numbers[0].toInt(),
        second = numbers[1].toInt()
    )
}

private const val LIST_DIVIDER = '|'
private const val LIST_ESCAPE = '\\'
private const val PAIR_DIVIDER = '*'
