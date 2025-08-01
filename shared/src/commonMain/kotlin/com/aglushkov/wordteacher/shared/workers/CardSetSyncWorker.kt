package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.resource.asLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.merge
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.service.SpaceCardSetService

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
    private val settings: SettingStore,
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
    private var pullErrorCount: Int = 0

    // last push/pull date, stored in settings
    var lastSyncDate: Instant = Instant.fromEpochMilliseconds(0)
    var lastSyncUserId: String = ""

    init {
        scope.launch {
            lastSyncDate = Instant.fromEpochMilliseconds(settings.long(LAST_SYNC_DATE_KEY, 0))
            lastSyncUserId = settings.string(LAST_SYNC_USER_ID, "")

            val authData = spaceAuthRepository.currentAuthData.asLoaded()?.data
            if (authData != null) {
                toState(State.PullRequired())
            } else {
                toAuthRequiredState()
            }

            // handle authorizing
            launch {
                spaceAuthRepository.authDataFlow.collect {
                    val authData = spaceAuthRepository.currentAuthData.asLoaded()?.data
                    if (state.value.innerState() is State.AuthRequired && authData != null) {
                        toState(State.PullRequired())
                    } else if (!it.isLoaded()) {
                        toAuthRequiredState()
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
                withContext(Dispatchers.IO) {
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

    private suspend fun pullInternal() {
        val authData = spaceAuthRepository.currentAuthData.asLoaded()?.data
        if (!canPull() || authData == null) {
//            waitUntilCanPull()
            return
        }

        if (lastSyncUserId.isNotEmpty() && lastSyncUserId != authData.user.id) {
            // last sync was with another user, so we need to delete the current remote cardsets
            // that will lead into loosing the latest unsynced changes in the remote cardsets
            // save new local cardsets in a new account
            updateLastSyncDate(Instant.fromEpochMilliseconds(0))
            databaseWorker.run { database ->
                database.cardSets.removeCardSets(database.cardSets.idsForRemoteCardSets())
            }
        }

        updateLastSyncUserId(authData.user.id)
        toState(State.Pulling)
        lastPullDate = timeSource.timeInstant()
        val newSyncDate: Instant?

        try {
            withContext(Dispatchers.IO) {
                val cardSetRemoteIds = databaseWorker.run { database ->
                    database.cardSets.remoteIds()
                }

                val pullResponse = spaceCardSetService.pull(cardSetRemoteIds, lastSyncDate).toOkResponse()
                newSyncDate = pullResponse.latestModificationDate

                databaseWorker.run { database ->
                    database.transaction {
                        val locallyUpdatedCardSetIds = database.cardSets.selectUpdatedCardSetsIds(lastSyncDate.toEpochMilliseconds())
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
                        database.cardSets.updateCardSetModificationDateForIds(newSyncDate.toEpochMilliseconds() + 1, locallyUpdatedCardSetIds)
                    }
                }

                updateLastSyncDate(newSyncDate)
            }

            if (state.value == State.Pulling) {
                toState(State.PushRequired())
            }
        } catch (e: Exception) {
            if (state.value == State.Pulling) {
                if (pullErrorCount >= 2) {
                    pullErrorCount = 0
                    toState(State.Idle)
                    scope.launch {
                        delay(PULL_AUTO_TRY_AGAIN_INTERVAL)
                        if (canPull()) {
                            toState(State.PullRequired())
                        }
                    }
                } else {
                    ++pullErrorCount
                    toState(State.PullRequired(e))
                }
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
            withContext(Dispatchers.IO) {
                var updatedCardSets: List<CardSet> = emptyList()

                databaseWorker.run { database ->
                    // get updated and not pushed cardsets
                    updatedCardSets = database.cardSets.selectUpdatedCardSets(lastSyncDate.toEpochMilliseconds())

                    val allCardSetsIds = updatedCardSets.map { it.id }
                    val setIdToCards = database.cardSets.selectSetIdsWithCards(allCardSetsIds).executeAsList().groupBy({it.first},{it.second})

                    updatedCardSets = updatedCardSets.map { it.copy(cards = setIdToCards[it.id].orEmpty()) }
                }

                val cardSetRemoteIds = databaseWorker.run { database ->
                    database.cardSets.remoteIds()
                }

                val pushResponse = spaceCardSetService.push(updatedCardSets, cardSetRemoteIds, lastSyncDate).toOkResponse()
                newSyncDate = pushResponse.latestModificationDate

                databaseWorker.run { database ->
                    database.transaction {
                        pushResponse.cardSetIds?.onEach { (creationId, remoteId) ->
                            database.cardSets.updateCardSetRemoteId(remoteId, creationId)
                        }

                        pushResponse.cardIds?.onEach { (creationId, remoteId) ->
                            database.cards.updateCardSetRemoteId(remoteId, creationId)
                        }
                    }
                }

                updateLastSyncDate(newSyncDate)
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

    private fun toAuthRequiredState() {
        toState(State.AuthRequired)
    }

    private fun updateLastSyncDate(newSyncDate: Instant) {
        settings[LAST_SYNC_DATE_KEY] = newSyncDate.toEpochMilliseconds()
        lastSyncDate = newSyncDate
    }

    private fun updateLastSyncUserId(id: String) {
        if (lastSyncUserId == id) {
            return
        }

        settings[LAST_SYNC_USER_ID] = id
        lastSyncUserId = id
    }

    fun canPush() = spaceAuthRepository.isAuthorized() && (state.value.innerState() is State.PushRequired || state.value.innerState() == State.Idle)

    suspend fun waitUntilDone() {
        state.takeWhile { it is State.Pulling || it is State.Pushing }.collect()
    }

    suspend fun pauseAndWaitUntilDone() {
        pause()
        waitUntilDone()
    }
}

private const val LAST_SYNC_DATE_KEY = "LAST_SYNC_DATE_KEY"
private const val LAST_SYNC_USER_ID = "LAST_SYNC_USER_ID"
private const val PULL_RETRY_INTERVAL = 5000L
private const val PUSH_RETRY_INTERVAL = 20000L
private const val PULL_AUTO_TRY_AGAIN_INTERVAL = PULL_RETRY_INTERVAL * 10
