package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.resource.asLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.service.SpaceCardSetService
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import kotlinx.datetime.asTimeSource

class CardSetSyncWorker(
    private val spaceAuthRepository: SpaceAuthRepository,
    private val spaceCardSetService: SpaceCardSetService,
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val timeSource: TimeSource,
    private val settings: FlowSettings,
) {
    private sealed interface State {
        object AuthRequired: State
        data class PullRequired(val e: Exception? = null): State
        object Pulling: State
        object Pushing: State
        object Idle: State
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val serialQueue = SerialQueue()
    private var state = MutableStateFlow<State>(State.AuthRequired)
    private var errpr: Exception? = null
    private var lastPullDate: Instant = Instant.fromEpochMilliseconds(0)

    //private var lastSyncModificationDate: Instant = Instant.fromEpochMilliseconds(0)

    init {
        // handle authorizing
        scope.launch {
            spaceAuthRepository.authDataFlow.collect {
                if (spaceAuthRepository.isAuthorized()) {
                    state.value = State.PullRequired()
                } else {
                    state.value = State.AuthRequired
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
        try {
            var lastModificationDate: Instant? = null
            val lastModificationDateLong = withContext(Dispatchers.Default) {
                serialQueue.sendAndWait {
                    databaseWorker.run {
                        database.cardSets.lastModificationDate()
                    }
                }
            }

            // TODO:
            //spaceCardSetService.pull()

            //TODO update modificationdate for unsent cardset

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
