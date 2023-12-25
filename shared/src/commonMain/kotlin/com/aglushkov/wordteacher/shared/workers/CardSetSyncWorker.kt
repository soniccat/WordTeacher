package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.merge
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.service.SpaceCardSetService
import com.russhwolf.settings.coroutines.FlowSettings
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant

class CardSetSyncWorker(
    private val spaceAuthRepository: SpaceAuthRepository,
    private val spaceCardSetService: SpaceCardSetService,
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val timeSource: TimeSource,
    private val settings: FlowSettings,
) {
    sealed interface State {
        object Initializing: State
        object AuthRequired: State
        data class PullRequired(val e: Exception? = null): State
        object Pulling: State
        data class PushRequired(val e: Exception? = null): State
        object Pushing: State
        object Idle: State
        data class Paused(val prevState: State): State

        fun resume(): State {
            return when (this) {
                is Paused -> prevState
                else -> this
            }
        }

        fun pause(): State {
            return when (this) {
                is Paused -> Paused(prevState)
                else -> Paused(this)
            }
        }

        fun toState(newState: State): State {
            return when (this) {
                is Paused -> Paused(newState)
                else -> newState
            }
        }

        fun isPausedOrPausedIdle(): Boolean {
            return when (this) {
                is Paused -> prevState == State.Idle
                is Idle -> true
                else -> false
            }
        }

        fun innerState(): State {
            return when (this) {
                is Paused -> prevState
                else -> this
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var state = MutableStateFlow<State>(State.Paused(State.Initializing))
    val stateFlow: Flow<State> = state
    private var lastPullDate: Instant = Instant.fromEpochMilliseconds(0)
    private var lastPushDate: Instant = Instant.fromEpochMilliseconds(0)

    // last push/pull date, stored in settings
    var lastSyncDate: Instant = Instant.fromEpochMilliseconds(0)

    init {
        scope.launch {
            lastSyncDate = Instant.fromEpochMilliseconds(settings.getLong(LAST_SYNC_DATE_KEY, 0))

            if (spaceAuthRepository.isAuthorized()) {
                toState(State.PullRequired())
            } else {
                toState(State.AuthRequired)
            }

            // handle authorizing
            launch {
                spaceAuthRepository.authDataFlow.collect {
                    if (state.value.innerState() is State.AuthRequired && spaceAuthRepository.isAuthorized()) {
                        toState(State.PullRequired())
                    } else if (!it.isLoaded()) {
                        toState(State.AuthRequired)
                    }
                }
            }

            // handle PullRequired
            launch {
                state.collect { st ->
                    if (st is State.PullRequired) {
                        while (state.value is State.PullRequired) {
                            val interval = timeSource.timeInstant() - lastPullDate
                            if (interval.inWholeSeconds <= PULL_RETRY_INTERVAL) {
                                delay(PULL_RETRY_INTERVAL - interval.inWholeSeconds)
                            }

                            if (state.value is State.PullRequired) {
                                pullInternal()
                            }
                        }
                    }
                }
            }

            // handle PushRequired
            launch {
                state.collect { st ->
                    if (st is State.PushRequired) {
                        while (state.value is State.PushRequired) {
                            val interval = timeSource.timeInstant() - lastPushDate
                            if (interval.inWholeMilliseconds <= PUSH_RETRY_INTERVAL) {
                                delay(PUSH_RETRY_INTERVAL - interval.inWholeMilliseconds)
                            }

                            if (state.value is State.PushRequired) {
                                pushInternal()
                            }
                        }
                    }
                }
            }

            // watch database changes
            launch {
                withContext(Dispatchers.Default) {
                    var pushRequestJob: Job? = null
                    combine(database.cardSets.changeFlow(), database.cards.changeFlow()) { a, b ->
                        a + b
                    }.collect {
                        pushRequestJob?.cancel()
                        pushRequestJob = launch(Dispatchers.Main) {
                            delay(1000)
                            if (state.value.isPausedOrPausedIdle()) {
                                toState(State.PushRequired())
                            }
                        }
                    }
                }
            }
        }
    }

    fun pause() {
        Logger.v("toState ${state.value.pause()}", "cardSetSyncWorker")
        state.update { it.pause() }
    }

    fun resume() {
        Logger.v("toState ${state.value.resume()}", "cardSetSyncWorker")
        state.update { it.resume() }
    }

    private fun toState(st: State) {
        Logger.v("toState ${state.value.toState(st)}", "cardSetSyncWorker")
        state.update { it.toState(st) }
    }

    fun pull() {
        if (!canPull()) {
            return
        }

        scope.launch {
            pullInternal()
        }
    }

    private suspend fun pullInternal() {
        if (!canPull()) {
//            waitUntilCanPull()
            return
        }

        toState(State.Pulling)
        lastPullDate = timeSource.timeInstant()
        val newSyncDate: Instant?

        try {
            withContext(Dispatchers.Default) {
                val cardSetRemoteIds = databaseWorker.run {
                    database.cardSets.remoteIds()
                }

                val pullResponse = spaceCardSetService.pull(cardSetRemoteIds, lastSyncDate).toOkResponse()
                newSyncDate = pullResponse.latestModificationDate

                databaseWorker.run {
                    database.transaction {
                        val dbCardSets = database.cardSets.selectShortCardSets()
                        val remoteIdToId = dbCardSets.associate { it.remoteId to it.id }

                        pullResponse.updatedCardSets?.map {
                            it.copy(id = remoteIdToId[it.remoteId] ?: 0L)
                        }?.map { remoteCardSet ->
                            dbCardSets.firstOrNull {
                                it.remoteId == remoteCardSet.remoteId
                            }?.let { dbCardSet ->
                                // as we got these cardSets from the back it means that remoteCardSet.modificationDate > lastSyncDate
                                if (dbCardSet.modificationDate > lastSyncDate) {
                                    // there're local and remote changes, need to merge
                                    val fullCardSet = database.cardSets.loadCardSetWithCards(dbCardSet.id)!!
                                    val mergedCardSet = fullCardSet.merge(remoteCardSet, newSyncDate)
                                    database.cardSets.updateCardSet(mergedCardSet)

                                } else if (dbCardSet.modificationDate <= lastSyncDate) {
                                    // there's only remote change
                                    database.cardSets.updateCardSet(remoteCardSet)
                                } else {
                                    Logger.e("Strange cardSet state")
                                }
                            } ?: run {
                                database.cardSets.insert(remoteCardSet)
                            }
                        }

                        pullResponse.deletedCardSetIds?.onEach { remoteCardSetId ->
                            dbCardSets.firstOrNull {
                                it.remoteId == remoteCardSetId
                            }?.let { dbCardSet ->
                                // delete here without checks as we sent all the local cardSet remote ids
                                database.cardSets.removeCardSet(dbCardSet.id)
                            }
                        }

                        // updates modificationDate for changed locally or just created but not pushed cardsets not to lose the changes
                        database.cardSets.shiftCardSetModificationDate(newSyncDate.toEpochMilliseconds() + 1000, lastSyncDate.toEpochMilliseconds())
                    }
                }

                settings.putLong(LAST_SYNC_DATE_KEY, newSyncDate.toEpochMilliseconds())
                lastSyncDate = newSyncDate
            }

            if (state.value == State.Pulling) {
                toState(State.PushRequired())
            }
        } catch (e: Exception) {
            if (state.value == State.Pulling) {
                toState(State.PullRequired(e))
            }
        }
    }

    fun canPull() = spaceAuthRepository.isAuthorized() && (state.value.innerState() is State.PullRequired || state.value.innerState() == State.Idle)

    fun push() {
        if (!canPush()) {
            return
        }

        scope.launch {
            pushInternal()
        }
    }

    private suspend fun pushInternal() {
        if (!canPush()) {
//            waitUntilCanPull()
            return
        }

        toState(State.Pushing)
        lastPushDate = timeSource.timeInstant()
        val newSyncDate: Instant?

        try {
            withContext(Dispatchers.Default) {
                var updatedCardSets: List<CardSet> = emptyList()

                databaseWorker.run {
                    // get updated and not pushed cardsets
                    updatedCardSets = database.cardSets.selectUpdatedCardSets(lastSyncDate.toEpochMilliseconds())

                    val allCardSetsIds = updatedCardSets.map { it.id }
                    val setIdToCards = database.cardSets.selectSetIdsWithCards(allCardSetsIds).executeAsList().groupBy({it.first},{it.second})

                    updatedCardSets = updatedCardSets.map { it.copy(cards = setIdToCards[it.id].orEmpty()) }
                }

                val cardSetRemoteIds = databaseWorker.run {
                    database.cardSets.remoteIds()
                }

                val pushResponse = spaceCardSetService.push(updatedCardSets, cardSetRemoteIds, lastSyncDate).toOkResponse()
                newSyncDate = pushResponse.latestModificationDate

                databaseWorker.run {
                    database.transaction {
                        pushResponse.cardSetIds?.onEach { (creationId, remoteId) ->
                            database.cardSets.updateCardSetRemoteId(remoteId, creationId)
                        }

                        pushResponse.cardIds?.onEach { (creationId, remoteId) ->
                            database.cards.updateCardSetRemoteId(remoteId, creationId)
                        }
                    }
                }

                settings.putLong(LAST_SYNC_DATE_KEY, newSyncDate.toEpochMilliseconds())
                lastSyncDate = newSyncDate
            }

            if (state.value == State.Pushing) {
                toState(State.Idle)
            }
        } catch (e: Exception) {
             if (state.value == State.Pushing) {
                 if (e is ErrorResponseException && e.statusCode == HttpStatusCode.Conflict.value) {
                     toState(State.PullRequired(e))
                 } else {
                     toState(State.PushRequired(e))
                 }
            }
        }
    }

    fun canPush() = spaceAuthRepository.isAuthorized() && (state.value.innerState() is State.PushRequired || state.value.innerState() == State.Idle)

//    private suspend fun waitUntilCanPull() {
//        state.takeWhile { !canPull() }.collect()
//    }

//    private suspend fun waitUntilNotPulling() {
//        state.takeWhile { it is State.Pulling }.collect()
//    }

    suspend fun waitUntilDone() {
        state.takeWhile { it is State.Pulling || it is State.Pushing }.collect()
    }

    suspend fun pauseAndWaitUntilDone() {
        pause()
        waitUntilDone()
    }
}

private const val LAST_SYNC_DATE_KEY = "LAST_SYNC_DATE_KEY"
private const val PULL_RETRY_INTERVAL = 5000L
private const val PUSH_RETRY_INTERVAL = 20000L
