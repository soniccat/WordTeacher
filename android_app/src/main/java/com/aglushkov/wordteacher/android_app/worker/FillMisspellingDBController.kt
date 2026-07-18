package com.aglushkov.wordteacher.android_app.worker

import android.content.Context
import android.os.storage.StorageManager
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkInfo.Companion.STOP_REASON_NOT_STOPPED
import androidx.work.WorkManager
import com.aglushkov.wordteacher.android_app.repository.NotificationPermissionRepository
import com.aglushkov.wordteacher.android_app.tasks.FILL_MISSPELLING_DATA_PROGRESS_KEY
import com.aglushkov.wordteacher.android_app.tasks.FillMisspellingDBWorker
import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.general.extensions.updateWithLoadedData
import com.aglushkov.wordteacher.shared.general.extensions.collectUntilDone
import com.aglushkov.wordteacher.shared.general.extensions.takeUntilLoadedOrErrorForVersion
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.isError
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.onData
import com.aglushkov.wordteacher.shared.general.resource.onError
import com.aglushkov.wordteacher.shared.general.resource.onLoaded
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.repository.suggestion.SymSpellRepository
import com.aglushkov.wordteacher.shared.repository.toggles.ToggleRepository
import com.aglushkov.wordteacher.shared.workers.FillMisspellingDBController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class FillMisspellingDBControllerImpl(
    private val context: Context,
    private val settings: SettingStore,
    private val lastVersion: Int,
    private val analytics: Analytics,
    private val notificationPermissionRepository: NotificationPermissionRepository,
    private val toggles: ToggleRepository,
): FillMisspellingDBController, SimpleResourceRepository<Unit, Unit>() {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val workManager = WorkManager.getInstance(context)
    private val workState = MutableStateFlow<Resource<WorkInfo>>(Resource.Uninitialized())

    override val isLoaded: Boolean
        get() = settings.int(MISSPELLING_FILLED_DB_VERSION_KEY, -1) == lastVersion

    override val loadingFlow: Flow<Resource<Float>>
        get() = if (isLoaded) {
                flowOf(Resource.Loaded(1.0f))
            } else {
                workState.map { work ->
                    work.map {
                        work.data()?.progress?.getFloat(FILL_MISSPELLING_DATA_PROGRESS_KEY, 0.0f)
                    }
                }
            }

    init {
        val currentVersion = settings.int(MISSPELLING_FILLED_DB_VERSION_KEY, -1)
        val isFilled = currentVersion == lastVersion
        if (isFilled) {
            stateFlow.updateWithLoadedData(Unit)
        } else if (!toggles.toggles.disableMisspellingDB) {
            startObserveWorkManager()
        }
    }

    override fun reset() {
        settings[MISSPELLING_FILLED_DB_VERSION_KEY] = -1
    }

    private fun startObserveWorkManager() = scope.launch {
        workManager.getWorkInfosForUniqueWorkFlow(FILL_MISSPELLING_DB_WORK).collect { workInfos ->
            if (workInfos.isNotEmpty()) {
                val workInfo = workInfos.first()
                workState.update {
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            it.toLoaded(workInfo)
                        }

                        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                            it.toError(
                                RuntimeException("StopReason: ${workInfo.stopReason}"),
                                true,
                                workInfo
                            )
                        }

                        else -> {
                            it.toLoading(workInfo)
                        }
                    }
                }
            }
        }
    }

    override fun loadIfNotLoaded() {
        if (toggles.toggles.disableMisspellingDB) {
            return
        }

        scope.launch {
            loadIfNotLoaded(Unit).collectUntilDone()
        }
    }

    private fun tryToAllocateRequiredSpace(): Boolean {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val uuid: UUID = StorageManager.UUID_DEFAULT

        return try {
            val allocatableBytes = storageManager.getAllocatableBytes(uuid)
            if (allocatableBytes >= MISSPELLING_DB_SIZE) {
                storageManager.allocateBytes(uuid, MISSPELLING_DB_SIZE)
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    override suspend fun loadInternal(arg: Unit): Unit {
        if (!tryToAllocateRequiredSpace()) {
            val message = "FillMisspellingDB.tryToAllocateRequiredSpace is false"
            val error = RuntimeException(message)
            analytics.send(
                AnalyticEvent.createErrorEvent(
                    message,
                    error,
                )
            )
            throw error
        }
        notificationPermissionRepository.loadIfNotLoaded(Unit).collectUntilDone()

        reset()
        workState.update { it.bumpVersion().toLoading() }
        enqueueWork()

        workState.takeUntilLoadedOrErrorForVersion().collectUntilDone().apply {
            onError {
                if (it !is CancellationException) {
                    val stopReason = data()?.stopReason
                    if (stopReason != STOP_REASON_NOT_STOPPED) {
                        analytics.send(
                            AnalyticEvent.createErrorEvent(
                                "FillMisspellingDB.error stopReason: ${data()?.stopReason ?: 0}",
                                it,
                            )
                        )
                    }
                }

                throw it // to propagate status to stateFlow
            }
            onData { markAsComplete() }
        }
    }

    fun markAsComplete() {
        settings[MISSPELLING_FILLED_DB_VERSION_KEY] = lastVersion
        analytics.send(
            AnalyticEvent.createActionEvent(
                "FillMisspellingDB.complete",
                mapOf("version" to lastVersion),
            )
        )
    }

    private fun enqueueWork() {
        workManager.beginUniqueWork(
            FILL_MISSPELLING_DB_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<FillMisspellingDBWorker>()
//                .setInitialDelay(1, TimeUnit.MINUTES)
//                .setConstraints(Constraints.Builder()
//                    .setRequiresBatteryNotLow(true)
//                    .setRequiresStorageNotLow(true)
//                    .build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        ).enqueue()
    }
}

private const val FILL_MISSPELLING_DB_WORK = "FILL_MISSPELLING_DB_WORK"
private const val MISSPELLING_FILLED_DB_VERSION_KEY = "MISSPELLING_FILLED_DB_VERSION_KEY"
private const val MISSPELLING_DB_SIZE = 1024*1024*100L
