package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.toOkResult
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.merge
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.service.SpaceCardSetService
import com.russhwolf.settings.coroutines.FlowSettings
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

        fun toPaused(): State {
            return when (this) {
                is Paused -> Paused(this)
                else -> this
            }
        }

        fun toState(newState: State): State {
            return when (this) {
                is Paused -> Paused(newState)
                else -> newState
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
        // handle initialising
        scope.launch {
            lastSyncDate = Instant.fromEpochMilliseconds(settings.getLong(LAST_SYNC_DATE_KEY))

            if (spaceAuthRepository.isAuthorized()) {
                toState(State.AuthRequired)
            } else {
                toState(State.PullRequired())
            }
        }

        // handle authorizing
        scope.launch {
            spaceAuthRepository.authDataFlow.collect {
                if (state.value != State.Initializing) {
                    if (spaceAuthRepository.isAuthorized()) {
                        toState(State.PullRequired())
                    } else {
                        toState(State.AuthRequired)
                    }
                }
            }
        }

        // handle PullRequired
        scope.launch {
            state.collect { st ->
                if (st is State.PullRequired) {
                    val interval = timeSource.getTimeInstant() - lastPullDate
                    if (interval.inWholeSeconds <= PULL_RETRY_INTERVAL) {
                        delay(PULL_RETRY_INTERVAL - interval.inWholeSeconds)
                    }

                    if (state.value is State.PullRequired) {
                        pullInternal()
                    }
                }
            }
        }

        // handle PushRequired
        scope.launch {
            state.collect { st ->
                if (st is State.PushRequired) {
                    val interval = timeSource.getTimeInstant() - lastPushDate
                    if (interval.inWholeSeconds <= PUSH_RETRY_INTERVAL) {
                        delay(PUSH_RETRY_INTERVAL - interval.inWholeSeconds)
                    }

                    if (state.value is State.PushRequired) {
                        pushInternal()
                    }
                }
            }
        }

        // handle Idle
        scope.launch {
            state.collect { st ->
                if (st is State.Idle) {
                    while (state.value == State.Idle) {
                        delay(PUSH_DELAY_INTERVAL)
                        if (canPush()) {
                            push()
                        }
                    }
                }
            }
        }
    }

    fun pause() {
        state.update {
            if (it is State.Paused) {
                it
            } else {
                State.Paused(it)
            }
        }
    }

    fun resume() {
        state.update { it.toPaused() }
    }

    private fun toState(st: State) {
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
        lastPullDate = timeSource.getTimeInstant()
        val newSyncDate: Instant?

        try {
            withContext(Dispatchers.Default) {
                val cardSetRemoteIds = databaseWorker.run {
                    database.cardSets.remoteIds()
                }

                val pullResponse = spaceCardSetService.pull(cardSetRemoteIds, lastSyncDate).toOkResult()
                newSyncDate = timeSource.getTimeInstant()

                databaseWorker.run {
                    database.transaction {
                        val dbCardSets = database.cardSets.selectShortCardSets()
                        val remoteIdToId = dbCardSets.associate { it.remoteId to it.id }

                        pullResponse.updatedCardSets.map {
                            it.copy(id = remoteIdToId[it.remoteId] ?: 0L)
                        }.map { remoteCardSet ->
                            dbCardSets.firstOrNull {
                                it.remoteId == remoteCardSet.remoteId
                            }?.let { dbCardSet ->
                                // as we got these cardSets from the back it means that remoteCardSet.modificationDate > lastSyncDate
                                if (dbCardSet.modificationDate > lastSyncDate) {
                                    // there're local and remote changes, need to merge
                                    val fullCardSet = database.cardSets.selectCardSet(dbCardSet.id).executeAsOne()
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

                        pullResponse.deletedCardSetIds.onEach { remoteCardSetId ->
                            dbCardSets.firstOrNull {
                                it.remoteId == remoteCardSetId
                            }?.let { dbCardSet ->
                                // delete here without checks as we sent all the local cardSet remote ids
                                database.cardSets.removeCardSet(dbCardSet.id)
                            }
                        }
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

    fun canPull() = spaceAuthRepository.isAuthorized() && (state.value is State.PullRequired || state.value == State.Idle)

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
        lastPushDate = timeSource.getTimeInstant()
        val newSyncDate: Instant?

        try {
            withContext(Dispatchers.Default) {
                var updatedCardSets: List<CardSet> = emptyList()
                var notPushedCardSets: List<CardSet> = emptyList()

                databaseWorker.run {
                    updatedCardSets = database.cardSets.selectUpdatedCardSets(lastSyncDate.toEpochMilliseconds())
                    notPushedCardSets = database.cardSets.selectWithoutRemoteId()

                    val allCardSetsIds = updatedCardSets.map { it.requireId() } + notPushedCardSets.map { it.requireId() }
                    val setIdToCards = database.cardSets.selectSetIdsWithCards(allCardSetsIds).executeAsList().groupBy({it.first},{it.second})

                    updatedCardSets = updatedCardSets.map { it.copy(cards = setIdToCards[it.id].orEmpty()) }
                    notPushedCardSets = notPushedCardSets.map { it.copy(cards = setIdToCards[it.id].orEmpty()) }
                }

                val cardSetRemoteIds = databaseWorker.run {
                    database.cardSets.remoteIds()
                }

                val pushResponse = spaceCardSetService.push(updatedCardSets + notPushedCardSets, cardSetRemoteIds, lastSyncDate).toOkResult()
                newSyncDate = timeSource.getTimeInstant()

                databaseWorker.run {
                    database.transaction {
                        pushResponse.cardSetIds.onEach { (creationId, remoteId) ->
                            database.cardSets.updateCardSetRemoteId(remoteId, newSyncDate.toEpochMilliseconds(), creationId)
                        }

                        pushResponse.cardIds.onEach { (creationId, remoteId) ->
                            database.cards.updateCardSetRemoteId(remoteId, newSyncDate.toEpochMilliseconds(), creationId)
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
            // TODO: handle pull required error code

            if (state.value == State.Pushing) {
                toState(State.PushRequired(e))
            }
        }
    }

    fun canPush() = spaceAuthRepository.isAuthorized() && (state.value is State.PushRequired || state.value == State.Idle)

    private suspend fun waitUntilCanPull() {
        state.takeWhile { !canPull() }.collect()
    }

    private suspend fun waitUntilNotPulling() {
        state.takeWhile { it is State.Pulling }.collect()
    }

    suspend fun waitUntilDone() {
        state.takeWhile { it !is State.Pulling && it !is State.Pushing }.collect()
    }

    suspend fun pauseAndWaitUntilDone() {
        pause()
        waitUntilDone()
    }
}

private const val LAST_SYNC_DATE_KEY = "LAST_SYNC_DATE_KEY"
private const val PULL_RETRY_INTERVAL = 5L
private const val PUSH_RETRY_INTERVAL = 15L
private const val PUSH_DELAY_INTERVAL = 30L