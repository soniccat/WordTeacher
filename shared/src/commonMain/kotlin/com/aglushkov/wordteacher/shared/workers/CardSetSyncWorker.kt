package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.toOkResult
import com.aglushkov.wordteacher.shared.model.merge
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.service.SpaceCardSetService
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.datetime.Instant

class CardSetSyncWorker(
    private val spaceAuthRepository: SpaceAuthRepository,
    private val spaceCardSetService: SpaceCardSetService,
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val timeSource: TimeSource,
    private val settings: FlowSettings,
) {
    private sealed interface State {
        object Initializing: State
        object AuthRequired: State
        data class PullRequired(val e: Exception? = null): State
        object Pulling: State
        object Pushing: State
        object Idle: State
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val serialQueue = SerialQueue()
    private var state = MutableStateFlow<State>(State.Initializing)
    private var errpr: Exception? = null
    private var lastPullDate: Instant = Instant.fromEpochMilliseconds(0)

    private var lastSyncDate: Instant = Instant.fromEpochMilliseconds(0) // from settings

    init {
        // handle initialising
        scope.launch {
            lastSyncDate = Instant.fromEpochMilliseconds(settings.getLong(LAST_SYNC_DATE_KEY))

            if (spaceAuthRepository.isAuthorized()) {
                state.value = State.AuthRequired
            } else {
                state.value = State.PullRequired()
            }
        }

        // handle authorizing
        scope.launch {
            spaceAuthRepository.authDataFlow.collect {
                if (state.value != State.Initializing) {
                    if (spaceAuthRepository.isAuthorized()) {
                        state.value = State.PullRequired()
                    } else {
                        state.value = State.AuthRequired
                    }
                }
            }
        }

        // handle initial pulling and pulling requests
        scope.launch {
            state.collect { st ->
                if (st is State.PullRequired) {
                    val interval = timeSource.getTimeInstant() - lastPullDate
                    if (interval.inWholeSeconds <= 60L) {
                        delay(60L - interval.inWholeSeconds)
                    }

                    if (state.value is State.PullRequired) {
                        pullInternal()
                    }
                }
            }
        }

        // handle pushing from time to time
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

        state.value = State.Pulling
        lastPullDate = timeSource.getTimeInstant()
        val newSyncDate: Instant?

        try {
            withContext(Dispatchers.Default) {
//                val lastModificationDateLong = databaseWorker.run {
//                    database.cardSets.lastModificationDate()
//                }
//                val lastModificationDate =
//                    lastModificationDateLong.takeIf { it != 0L }?.let { milliseconds ->
//                        Instant.fromEpochMilliseconds(milliseconds)
//                    }
                val cardSetRemoteIds = databaseWorker.run {
                    database.cardSets.remoteIds()
                }

                val pullResponse = spaceCardSetService.pull(cardSetRemoteIds, lastSyncDate).toOkResult()
                // as we pull we store max remote date here, we store max local date on push
                newSyncDate = timeSource.getTimeInstant() //pullResponse.updatedCardSets.maxOfOrNull { it.modificationDate } ?: lastSyncDate

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

                                } else if (dbCardSet.modificationDate < lastSyncDate) {
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

                // NO: //TODO update modificationdate for unsent cardset in push
                settings.putLong(LAST_SYNC_DATE_KEY, newSyncDate.toEpochMilliseconds())
            }

            if (state.value == State.Pulling) {
                state.value = State.Idle
            }
        } catch (e: Exception) {
            if (state.value == State.Pulling) {
                state.value = State.PullRequired(e)
            }
        }
    }

    private fun canPull() = spaceAuthRepository.isAuthorized() && (state.value is State.PullRequired || state.value == State.Idle)

    private suspend fun waitUntilCanPull() {
        state.takeWhile { !canPull() }.collect()
    }
}

private const val LAST_SYNC_DATE_KEY = "LAST_SYNC_DATE_KEY"