package com.aglushkov.wordteacher.shared.repository.db

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.TransactionWithReturn
import app.cash.sqldelight.TransactionWithoutReturn
import com.aglushkov.wordteacher.db.DBCard
import com.aglushkov.wordteacher.db.DBNLPSentence
import com.aglushkov.wordteacher.maindb.MainDB
import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.general.FileOpenController
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.extensions.firstLong
import com.aglushkov.wordteacher.shared.model.CardProgress
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.merge
import com.aglushkov.wordteacher.shared.general.resource.tryInResource
import com.aglushkov.wordteacher.shared.general.toLong
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.*
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.TokenSpan
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path
import kotlin.time.ExperimentalTime

class AppDatabase(
    driverFactory: DatabaseDriverFactory,
    private val timeSource: TimeSource
) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val defaultScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val driver = driverFactory.createMainDBDriver()
    private var db = MainDB(
        driver,
        DBCardAdapter = DBCard.Adapter(
            WordPartOfSpeechAdapter(),
            StringListAdapter(),
            StringListAdapter(),
            StringListAdapter(),
            StringListAdapter(),
            CardSpanListAdapter(),
            CardSpanListAdapter(),
            StringListAdapter(),
            WordTeacherWordAudioFileAdapter(),
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
        explicitNulls = false
    }

    init {
        create()
    }

    suspend fun waitUntilInitialized() = state.first { it.isLoaded() }

    fun create() {
        state.value = Resource.Loading(this@AppDatabase)
        mainScope.launch(Dispatchers.IO) {
            try {
                createDb()
                state.value = Resource.Loaded(this@AppDatabase)
            } catch (e: Exception) {
                state.value = Resource.Error(e, true)
            }
        }
    }

    private fun createDb() {
        MainDB.Schema.create(driver)
    }

    fun transaction(
        noEnclosing: Boolean = false,
        body: TransactionWithoutReturn.() -> Unit
    ) = db.transaction(noEnclosing, body)

    fun <R> transactionWithResult(
        noEnclosing: Boolean = false,
        body: TransactionWithReturn<R>.() -> R
    ) = db.transactionWithResult(noEnclosing, body)

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

        fun removeAll() = db.dBNLPSentenceQueries.removeAll()
    }

    inner class Articles {
        fun insert(name: String, date: Long, link: String?, style: ArticleStyle) =
            db.dBArticleQueries.insert(name, date, encodeStyle(style), link, false)
        fun insertedArticleId() = db.dBArticleQueries.lastInsertedRowId().firstLong().value

        fun selectAllShortArticles() = db.dBArticleQueries.selectShort { id, name, date, link, isRead ->
            ShortArticle(id, name, date, link, isRead)
        }

        fun selectArticle(anId: Long) = combine(
            db.dBArticleQueries.selectArticle(anId).asFlow(),
            sentencesNLP.selectForArticle(anId).asFlow(),
        ) { f1, f2 ->
            val article = f1.executeAsOneOrNull()
            val sentences = f2.executeAsList().map { it.toNLPSentence() }
            article?.let {
                Article(article.id, article.name, article.date, article.link, article.isRead, sentences, decodeStyle(article.style))
            }
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

        fun setIsRead(id: Long, isRead: Boolean) {
            db.dBArticleQueries.setIsRead(isRead, id)
        }
    }

    inner class CardSets {
        val cardSetInsertedFlow = MutableSharedFlow<CardSet>()

        fun insert(name: String, date: Long, infoSource: String? = null) = db.dBCardSetQueries.insert(name, date, date, uuid4().toString(), "", "", infoSource, false)

        private fun insertedCardSetId() = db.dBCardSetQueries.lastInsertedRowId().firstLong()

        fun selectShortCardSets(): List<ShortCardSet> {
            return db.dBCardSetQueries.selectAll(mapper = { id, name, date, modificationDate, creationId, remoteId, infoDescription, infoSource, isAvailableInSearch ->
                ShortCardSet(id, name, Instant.fromEpochMilliseconds(date), Instant.fromEpochMilliseconds(modificationDate), 0f, 0f, creationId, remoteId, terms = emptyList())
            }).executeAsList()
        }

        fun selectUpdatedCardSets(afterDate: Long): List<CardSet> = db.dBCardSetQueries.selectUpdated(afterDate, mapper = { id, name, date, modificationDate, creationId, remoteId, infoDescription, infoSource, isAvailableInSearch ->
            CardSet(id, remoteId, name, Instant.fromEpochMilliseconds(date), Instant.fromEpochMilliseconds(modificationDate), emptyList(), emptyList(), creationId, CardSetInfo(infoDescription, infoSource), isAvailableInSearch)
        }).executeAsList()

        fun selectUpdatedCardSetsIds(sinceDate: Long) =
            db.dBCardSetQueries.selectUpdatedIds(sinceDate).executeAsList()

        fun selectWithoutRemoteId(): List<CardSet> = db.dBCardSetQueries.selectWithoutRemoteId(mapper = { id, name, date, modificationDate, creationId, remoteId, infoDescription, infoSource, isAvailableInSearch ->
            CardSet(id, remoteId, name, Instant.fromEpochMilliseconds(date), Instant.fromEpochMilliseconds(modificationDate), emptyList(), emptyList(), creationId, CardSetInfo(infoDescription, infoSource), isAvailableInSearch)
        }).executeAsList()

        fun changeFlow(): Flow<Int> {
            var changeCount = 0
            return db.dBCardSetQueries.selectAllIds().asFlow().map { ++changeCount }
        }

        fun selectAll(): Flow<Resource<List<ShortCardSet>>> {
            // TODO: reorganize db to pull progress from it instead of loading all the cards
            val shortCardSetsFlow = db.dBCardSetQueries.selectAll(mapper = { id, name, date, modificationDate, creationId, remoteId, infoDescription, infoSource, isAvailableInSearch ->
                ShortCardSet(id, name, Instant.fromEpochMilliseconds(date), Instant.fromEpochMilliseconds(modificationDate), 0f, 0f, creationId, remoteId, emptyList())
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
                                totalProgress = cards?.totalProgress() ?: 0f,
                                terms = cards.orEmpty().filter { it.progress.isReadyToLearn(timeSource) }.map { it.term }
                            )
                        }
                    }
                }
            )
        }

        fun selectCardSetWithoutCards(id: Long) = db.dBCardSetQueries.selectCardSet(id) { id, name, date, modificationDate, creationId, remoteId, infoDescription, infoSource, isAvailableInSearch ->
            CardSet(id, remoteId, name, Instant.fromEpochMilliseconds(date), Instant.fromEpochMilliseconds(modificationDate), emptyList(), emptyList(), creationId, CardSetInfo(infoDescription, infoSource), isAvailableInSearch)
        }

        fun loadCardSetWithCards(id: Long): CardSet? {
            val cardSet = cardSets.selectCardSetWithoutCards(id).executeAsOneOrNull() ?: return null
            val cards = cards.selectCards(id).executeAsList()
            return cardSet.copy(cards = cards)
        }

        fun removeCardSet(cardSetId: Long) {
            db.transaction {
                db.dBCardQueries.removeCardsBySetId(cardSetId)
                db.dBCardSetToCardRelationQueries.removeCardSet(cardSetId)
                db.dBCardSetQueries.removeCardSet(cardSetId)
            }
        }

        fun removeCardSets(cardSetIds: List<Long>) {
            db.transaction {
                db.dBCardQueries.removeCardsBySetIds(cardSetIds)
                db.dBCardSetToCardRelationQueries.removeCardSets(cardSetIds)
                db.dBCardSetQueries.removeCardSets(cardSetIds)
            }
        }

        fun selectAllSetIdsWithCards() = db.dBCardSetToCardRelationQueries.selectAllSetIdsWithCards { setId, id, date, term, partOfSpeech, transcription, definitions, synonyms, examples, progressLevel, progressLastMistakeCount, progressLastLessonDate, definitionTermSpans, exampleTermSpans, editDate, spanUpdateDate, modificationDate, creationId, remoteId, termFrequency, labels, audioFiles ->
            setId to cards.optionalCardMapper().invoke(id, date, term, partOfSpeech, transcription, definitions, synonyms, examples, progressLevel, progressLastMistakeCount, progressLastLessonDate, definitionTermSpans, exampleTermSpans, editDate, spanUpdateDate, modificationDate, creationId, remoteId, termFrequency, labels, audioFiles)
        }

        fun selectSetIdsWithCards(setIds: List<Long>) = db.dBCardSetToCardRelationQueries.selectSetIdsWithCards(setIds) { setId, id, date, term, partOfSpeech, transcription, definitions, synonyms, examples, progressLevel, progressLastMistakeCount, progressLastLessonDate, definitionTermSpans, exampleTermSpans, editDate, spanUpdateDate, modificationDate, creationId, remoteId, termFrequency, labels, audioFiles ->
            setId to cards.optionalCardMapper().invoke(id, date, term, partOfSpeech, transcription, definitions, synonyms, examples, progressLevel, progressLastMistakeCount, progressLastLessonDate, definitionTermSpans, exampleTermSpans, editDate, spanUpdateDate, modificationDate, creationId, remoteId, termFrequency, labels, audioFiles)
        }

        fun lastModificationDate() = db.dBCardSetQueries.lastModificationDate().executeAsList().firstOrNull()?.MAX ?: 0L

        fun remoteIds() = db.dBCardSetQueries.selectRemoteIds().executeAsList()

        fun idsForRemoteCardSets() = db.dBCardSetQueries.selectIdsForRemoteCardSets().executeAsList()

        fun insert(cardSet: CardSet): CardSet {
            var insertedCarSetId: Long = 0
            db.transaction {
                db.dBCardSetQueries.insert(
                    name = cardSet.name,
                    date = cardSet.creationDate.toEpochMilliseconds(),
                    modificationDate = cardSet.modificationDate.toEpochMilliseconds(),
                    creationId = cardSet.creationId,
                    remoteId = cardSet.remoteId,
                    infoDescription = cardSet.info.description,
                    infoSource = cardSet.info.source,
                    isAvailableInSearch = cardSet.isAvailableInSearch,
                )
                insertedCarSetId = insertedCardSetId().value!!
                cardSet.cards.onEach { card ->
                    cards.insertCard(insertedCarSetId, card, card.creationDate, card.modificationDate)
                }
            }

            defaultScope.launch {
                cardSetInsertedFlow.emit(cardSet)
            }

            return cardSet.copy(
                id = insertedCarSetId
            )
        }

        fun updateCardSet(
            cardSet: CardSet
        ): CardSet {
            db.transaction {
                updateCardSetInfo(cardSet)

                val currentCards = cards.selectCards(cardSet.id).executeAsList()
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
                    cards.insertCard(cardSet.id, card, card.creationDate, card.modificationDate)
                }
            }

            return cardSet
        }

        fun updateCardSetInfo(cardSet: CardSet) {
            updateCardSetInfo(
                id = cardSet.id,
                name = cardSet.name,
                date = cardSet.creationDate.toEpochMilliseconds(),
                modificationDate = timeSource.timeInMilliseconds(),
                creationId = cardSet.creationId,
                remoteId = cardSet.remoteId,
                infoDescription = cardSet.info.description,
                infoSource = cardSet.info.source,
                isAvailableInSearch = cardSet.isAvailableInSearch
            )
        }

        private fun updateCardSetInfo(
            id: Long,
            name: String,
            date: Long,
            modificationDate: Long,
            creationId: String,
            remoteId: String,
            infoDescription: String,
            infoSource: String?,
            isAvailableInSearch: Boolean
        ) = db.dBCardSetQueries.updateCardSet(
            name = name,
            date = date,
            modificationDate = modificationDate,
            creationId = creationId,
            remoteId = remoteId,
            id = id,
            infoDescription = infoDescription,
            infoSource = infoSource,
            isAvailableInSearch = isAvailableInSearch,
        )

        fun updateCardSetRemoteId(remoteId: String, creationId: String) =
            db.dBCardSetQueries.updateCardSetRemoteId(remoteId, creationId)

        fun updateCardSetModificationDateForIds(toDate: Long, ids: List<Long>) =
            db.dBCardSetQueries.updateCardSetModificationDateForIds(toDate, toDate, ids)
    }

    inner class Cards {
        fun selectAllCardIds() = db.dBCardQueries.selectAllCardIds()
        fun selectAllCards() = db.dBCardQueries.selectAllCards(mapper = cardMapper())
        fun cardChangeFlow(): Flow<Int> {
            var changeCount = 0
            return db.dBCardQueries.selectAllCardIds().asFlow().map { ++changeCount }
        }
        fun cardSetToCardRelationChangeFlow(): Flow<Int> {
            var changeCount = 0
            return db.dBCardSetToCardRelationQueries.selectAllIds().asFlow().map { ++changeCount }
        }
        fun changeFlow(): Flow<Int> = combine(
            cardChangeFlow(),
            cardSetToCardRelationChangeFlow(),
        ) { a, b ->
            a + b
        }

        fun selectCardsWithOutdatedSpans() =
            db.dBCardQueries.selectCardsWithOutdatedSpans()

        fun selectCardsWithUndefinedFrequency() =
            db.dBCardQueries.selectCardsWithUndefinedFrequency()

        fun selectCards(ids: List<Long>) = db.dBCardQueries.selectCards(
            ids,
            cardMapper()
        )

        fun selectCards(setId: Long) = db.dBCardSetToCardRelationQueries.selectCards(
            setId,
            mapper = optionalCardMapper()
        )

        fun cardMapper(): (
            id: Long,
            date: Long,
            term: String,
            partOfSpeech: WordTeacherWord.PartOfSpeech,
            transcriptions: List<String>?,
            definitions: List<String>,
            synonyms: List<String>,
            examples: List<String>,
            progressLevel: Long?,
            progressLastMistakeCount: Long?,
            progressLastLessonDate: Long?,
            definitionTermSpans: List<List<CardSpan>>,
            exampleTermSpans: List<List<CardSpan>>,
            needToUpdateDefinitionSpans: Long,
            needToUpdateExampleDefinitionSpans: Long,
            modificationDate: Long,
            creationId: String,
            remoteId: String,
            termFrequency: Double,
            labels: List<String>,
            audioFiles: List<WordTeacherWord.AudioFile>,
        ) -> Card =
            { id, date, term, partOfSpeech, transcriptions, definitions, synonyms, examples, progressLevel, progressLastMistakeCount, progressLastLessonDate, definitionTermSpans, exampleTermSpans, needToUpdateDefinitionSpans, needToUpdateExampleSpans, modificationDate, creationId, remoteId, termFrequency, labels, audioFiles ->
                Card(
                    id,
                    remoteId,
                    Instant.fromEpochMilliseconds(date),
                    Instant.fromEpochMilliseconds(modificationDate),
                    term,
                    definitions,
                    labels,
                    definitionTermSpans,
                    partOfSpeech,
                    transcriptions.orEmpty(),
                    audioFiles = audioFiles,
                    synonyms,
                    examples,
                    exampleTermSpans,
                    progress = CardProgress(
                        progressLevel?.toInt() ?: 0,
                        progressLastMistakeCount?.toInt() ?: 0,
                        progressLastLessonDate.takeIf { it != 0L }?.let {
                            Instant.fromEpochMilliseconds(it)
                        }

                    ),
                    needToUpdateDefinitionSpans = needToUpdateDefinitionSpans != 0L,
                    needToUpdateExampleSpans = needToUpdateExampleSpans != 0L,
                    creationId = creationId,
                    termFrequency = termFrequency,
                )
            }


        fun optionalCardMapper(): (
            id: Long?,
            date: Long?,
            term: String?,
            partOfSpeech: WordTeacherWord.PartOfSpeech?,
            transcriptions: List<String>?,
            definitions: List<String>?,
            synonyms: List<String>?,
            examples: List<String>?,
            progressLevel: Long?,
            progressLastMistakeCount: Long?,
            progressLastLessonDate: Long?,
            definitionTermSpans: List<List<CardSpan>>?,
            exampleTermSpans: List<List<CardSpan>>?,
            needToUpdateDefinitionSpans: Long?,
            needToUpdateExampleDefinitionSpans: Long?,
            modificationDate: Long?,
            creationId: String?,
            remoteId: String?,
            termFrequency: Double?,
            labels: List<String>?,
            audioFiles: List<WordTeacherWord.AudioFile>?,
        )  -> Card =
            { id, date, term, partOfSpeech, transcriptions, definitions, synonyms, examples, progressLevel, progressLastMistakeCount, progressLastLessonDate, definitionTermSpans, exampleTermSpans, needToUpdateDefinitionSpans, needToUpdateExampleSpans, modificationDate, creationId, remoteId, termFrequency, labels, audioFiles ->
                Card(
                    id!!,
                    remoteId.orEmpty(),
                    Instant.fromEpochMilliseconds(date!!),
                    Instant.fromEpochMilliseconds(modificationDate!!),
                    term!!,
                    definitions!!,
                    labels!!,
                    definitionTermSpans!!,
                    partOfSpeech ?: WordTeacherWord.PartOfSpeech.Undefined,
                    transcriptions.orEmpty(),
                    audioFiles = audioFiles.orEmpty(),
                    synonyms.orEmpty(),
                    examples.orEmpty(),
                    exampleTermSpans.orEmpty(),
                    progress = CardProgress(
                        progressLevel?.toInt() ?: 0,
                        progressLastMistakeCount?.toInt() ?: 0,
                        progressLastLessonDate.takeIf { it != 0L }?.let {
                            Instant.fromEpochMilliseconds(it)
                        }

                    ),
                    needToUpdateDefinitionSpans = needToUpdateDefinitionSpans != 0L,
                    needToUpdateExampleSpans = needToUpdateExampleSpans != 0L,
                    creationId = creationId!!,
                    termFrequency = termFrequency!!,
                )
            }

        fun insertCard(
            setId: Long,
            term: String = "",
            creationDate: Instant = Instant.fromEpochMilliseconds(0),
            modificationDate: Instant = creationDate,
            definitions: List<String> = listOf(),
            labels: List<String> = listOf(),
            definitionTermSpans: List<List<CardSpan>> = listOf(),
            partOfSpeech: WordTeacherWord.PartOfSpeech = WordTeacherWord.PartOfSpeech.Undefined,
            transcriptions: List<String>? = emptyList(),
            synonyms: List<String> = mutableListOf(),
            examples: List<String> = mutableListOf(),
            exampleTermSpans: List<List<CardSpan>> = listOf(),
            progress: CardProgress = CardProgress.EMPTY,
            needToUpdateDefinitionSpans: Boolean = false,
            needToUpdateExampleSpans: Boolean = false,
            creationId: String = uuid4().toString(),
            termFrequency: Double? = null,
            audioFiles: List<WordTeacherWord.AudioFile> = emptyList()
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
                transcriptions = transcriptions.orEmpty(),
                synonyms = synonyms,
                examples = examples,
                exampleTermSpans = exampleTermSpans,
                progress = progress,
                needToUpdateDefinitionSpans = needToUpdateDefinitionSpans,
                needToUpdateExampleSpans = needToUpdateExampleSpans,
                creationId = creationId,
                termFrequency = termFrequency ?: UNDEFINED_FREQUENCY,
                labels = labels,
                audioFiles = audioFiles,
            )

            cards.insertCard(setId, newCard)
            newCard = newCard.copy(id = cards.insertedCardId().value!!)
            return newCard
        }

        fun insertCard(setId: Long, newCard: Card) {
            insertCard(setId, newCard, newCard.creationDate, newCard.modificationDate)
        }

        fun insertCard(setId: Long, card: Card, creationDate: Instant, modificationDate: Instant) {
            cards.insertCardInternal(
                setId = setId,
                creationDate = creationDate.toEpochMilliseconds(),
                modificationDate = modificationDate.toEpochMilliseconds(),
                term = card.term,
                definitions = card.definitions,
                definitionTermSpans = card.definitionTermSpans,
                partOfSpeech = card.partOfSpeech,
                transcriptions = card.transcriptions,
                synonyms = card.synonyms,
                examples = card.examples,
                exampleTermSpans = card.exampleTermSpans,
                progress = card.progress,
                needToUpdateDefinitionSpans = card.needToUpdateDefinitionSpans,
                needToUpdateExampleSpans = card.needToUpdateExampleSpans,
                creationId = card.creationId,
                remoteId = card.remoteId,
                termFrequency = card.termFrequency,
                labels = card.labels,
                audioFiles = card.audioFiles,
            )
        }

        private fun insertCardInternal(
            setId: Long,
            creationDate: Long,
            modificationDate: Long,
            term: String,
            definitions: List<String>,
            labels: List<String>,
            definitionTermSpans: List<List<CardSpan>> = listOf(),
            partOfSpeech: WordTeacherWord.PartOfSpeech,
            transcriptions: List<String>?,
            synonyms: List<String>,
            examples: List<String>,
            exampleTermSpans: List<List<CardSpan>> = listOf(),
            progress: CardProgress,
            needToUpdateDefinitionSpans: Boolean,
            needToUpdateExampleSpans: Boolean,
            creationId: String,
            remoteId: String,
            termFrequency: Double,
            audioFiles: List<WordTeacherWord.AudioFile>,
        ) {
            db.transaction {
                db.dBCardSetQueries.updateCardSetModificationDate(modificationDate, setId)

                db.dBCardQueries.insert(
                    creationDate,
                    term,
                    partOfSpeech,
                    transcriptions,
                    definitions,
                    synonyms,
                    examples,
                    progress.currentLevel.toLong(),
                    progress.lastMistakeCount.toLong(),
                    progress.lastLessonDate?.toEpochMilliseconds() ?: 0L,
                    definitionTermSpans,
                    exampleTermSpans,
                    needToUpdateDefinitionSpans.toLong(),
                    needToUpdateExampleSpans.toLong(),
                    modificationDate,
                    creationId,
                    remoteId,
                    termFrequency,
                    labels,
                    audioFiles
                )

                val cardId = db.dBCardQueries.lastInsertedRowId().firstLong().value!!
                db.dBCardSetToCardRelationQueries.insert(setId, cardId)
            }
        }

        fun updateCard(
            card: Card,
            modificationDate: Long?
        ): Card {
            updateCard(
                cardId = card.id,
                remoteId = card.remoteId,
                creationDate = card.creationDate.toEpochMilliseconds(),
                modificationDate = modificationDate ?: card.modificationDate.toEpochMilliseconds(),
                term = card.term,
                definitions = card.definitions,
                labels = card.labels,
                definitionTermSpans = card.definitionTermSpans,
                partOfSpeech = card.partOfSpeech,
                transcriptions = card.transcriptions,
                synonyms = card.synonyms,
                examples = card.examples,
                exampleTermSpans = card.exampleTermSpans,
                progressLevel = card.progress.currentLevel,
                progressLastMistakeCount = card.progress.lastMistakeCount,
                progressLastLessonDate = card.progress.lastLessonDate?.toEpochMilliseconds() ?: 0L,
                needToUpdateDefinitionSpans = card.needToUpdateDefinitionSpans.toLong(),
                needToUpdateExampleSpans = card.needToUpdateExampleSpans.toLong(),
                audioFiles = card.audioFiles,
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
            labels: List<String>,
            definitionTermSpans: List<List<CardSpan>>,
            partOfSpeech: WordTeacherWord.PartOfSpeech,
            transcriptions: List<String>?,
            synonyms: List<String>,
            examples: List<String>,
            exampleTermSpans: List<List<CardSpan>>,
            progressLevel: Int,
            progressLastMistakeCount: Int,
            progressLastLessonDate: Long,
            needToUpdateDefinitionSpans: Long,
            needToUpdateExampleSpans: Long,
            audioFiles: List<WordTeacherWord.AudioFile>,
        ) {
            db.transaction {
                db.dBCardSetQueries.selectCardSetIdByCardId(cardId).executeAsOneOrNull()?.let { cardSetId ->
                    db.dBCardSetQueries.updateCardSetModificationDate(modificationDate, cardSetId)
                }

                db.dBCardQueries.updateCard(
                    creationDate,
                    term,
                    partOfSpeech,
                    transcriptions,
                    definitions,
                    synonyms,
                    examples,
                    progressLevel.toLong(),
                    progressLastMistakeCount.toLong(),
                    progressLastLessonDate,
                    definitionTermSpans,
                    exampleTermSpans,
                    needToUpdateDefinitionSpans,
                    needToUpdateExampleSpans,
                    modificationDate,
                    remoteId,
                    labels,
                    audioFiles,
                    cardId,
                )
            }
        }

        private fun insertedCardId() = db.dBCardSetQueries.lastInsertedRowId().firstLong()

        fun removeCard(
            cardId: Long,
            modificationDate: Long
        ) {
            db.transaction {
                db.dBCardSetQueries.selectCardSetIdByCardId(cardId).executeAsOneOrNull()?.let { cardSetId ->
                    db.dBCardSetQueries.updateCardSetModificationDate(modificationDate, cardSetId)
                }

                db.dBCardSetToCardRelationQueries.removeCard(cardId)
                db.dBCardQueries.removeCard(cardId)
            }
        }

        fun updateCardSetRemoteId(remoteId: String, creationId: String) =
            db.dBCardQueries.updateCardRemoteId(remoteId, creationId)

        // here we don't need to care of cardSet modificationDate as frequency is stored locally
        fun updateCardFrequency(id: Long, frequency: Double) =
            db.dBCardQueries.updateCardFrequency(frequency, id)
    }

    inner class Notes {
        fun insert(date: Long, text: String) = db.dBNoteQueries.insert(date, text)
        fun insertedNoteId() = db.dBNoteQueries.lastInsertedRowId().firstLong()
        fun selectAll() = db.dBNoteQueries.selectAll(mapper = ::Note)
        fun removeNote(id: Long) = db.dBNoteQueries.removeNote(id)
        fun removeAll() = db.dBNoteQueries.removeAll()
        fun updateNote(id: Long, text: String) = db.dBNoteQueries.update(text, id)
    }


    inner class WordFrequencyUpdateHandler(
        private val analytics: Analytics,
    ): FileOpenController.SuccessHandler {
        override fun prepare(path: Path): Boolean {
            return true
        }

        override fun handle(path: Path): Boolean {
            analytics.send(AnalyticEvent.createActionEvent("FileOpenController.success.wordFrequencyDB",
                mapOf("name" to path.name)))
            db.dBCardQueries.resetCardFrequencies()
            return true
        }
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
private class WordPartOfSpeechAdapter: ColumnAdapter<WordTeacherWord.PartOfSpeech, String> {
    override fun decode(databaseValue: String): WordTeacherWord.PartOfSpeech {
        return WordTeacherWord.PartOfSpeech.valueOf(databaseValue)
    }

    override fun encode(value: WordTeacherWord.PartOfSpeech): String {
        return value.name
    }
}

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

private class CardSpanListAdapter: ColumnAdapter<List<List<CardSpan>>, String> {
    private val outerAdapter = StringListAdapter()
    private val innerAdapter = StringListAdapter(
        divider = '#',
        escape = '$'
    )

    override fun decode(databaseValue: String): List<List<CardSpan>> =
        outerAdapter.decode(databaseValue).map { sentence ->
            innerAdapter.decode(sentence).map { stringSpan ->
                stringSpan.decodePair()
            }
        }

    override fun encode(value: List<List<CardSpan>>): String =
        outerAdapter.encode(
            value.map { listOfSpans ->
                innerAdapter.encode(
                    listOfSpans.map { it.encode() }
                )
            }
        )
}

private class WordTeacherWordAudioFileAdapter: ColumnAdapter<List<WordTeacherWord.AudioFile>, String> {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    override fun decode(databaseValue: String): List<WordTeacherWord.AudioFile> {
        if (databaseValue.isEmpty()) {
            return emptyList()
        }
        return json.decodeFromString(databaseValue)
    }

    override fun encode(value: List<WordTeacherWord.AudioFile>): String {
        return json.encodeToString(value)
    }
}

private fun CardSpan.encode(): String =
    "$start$PAIR_DIVIDER$end"

private fun String.decodePair(): CardSpan {
    val numbers = split(PAIR_DIVIDER)
    return CardSpan(
        start = numbers[0].toInt(),
        end = numbers[1].toInt()
    )
}

private const val LIST_DIVIDER = '|'
private const val LIST_ESCAPE = '\\'
private const val PAIR_DIVIDER = '*'
