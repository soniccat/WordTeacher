package com.aglushkov.wordteacher.android_app.worker

import android.content.Context
import android.os.storage.StorageManager
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.aglushkov.wordteacher.android_app.repository.NotificationPermissionRepository
import com.aglushkov.wordteacher.android_app.tasks.FillMisspellingDBWorker
import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.general.extensions.updateWithLoadedData
import com.aglushkov.wordteacher.shared.general.extensions.collectUntilDone
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.onLoaded
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.workers.FillMisspellingDBController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
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
): FillMisspellingDBController, SimpleResourceRepository<Boolean, Unit>() {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val workManager = WorkManager.getInstance(context)
    private val workState = MutableStateFlow<Resource<WorkInfo>>(Resource.Uninitialized())

    override val isReady: Boolean
        get() {
            return settings.boolean(MISSPELLING_FILLED_DB_VERSION_KEY, false)
        }

    init {
        val currentVersion = settings.int(MISSPELLING_FILLED_DB_VERSION_KEY, -1)
        val isFilled = currentVersion == lastVersion
        if (isFilled) {
            stateFlow.updateWithLoadedData(true)
        } else {
            startObserveWorkManager()
        }
    }

    private fun startObserveWorkManager() = scope.launch {
        workManager.getWorkInfosForUniqueWorkFlow(FILL_MISSPELLING_DB_WORK).collect {
            if (it.isNotEmpty()) {
                val workInfo = it.first()
                workState.update {
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            Resource.Loaded(workInfo)
                        }

                        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                            Resource.Error(
                                RuntimeException("StopReason: ${workInfo.stopReason}"),
                                true,
                                workInfo
                            )
                        }

                        else -> {
                            Resource.Loading(workInfo)
                        }
                    }
                }
            }
        }
    }

    fun loadIfNotLoaded() {
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

    override suspend fun loadInternal(arg: Unit): Boolean {
        if (!tryToAllocateRequiredSpace()) {
            return false
        }
        notificationPermissionRepository.loadIfNotLoaded(Unit).collectUntilDone()

        workState.update { it.toLoading() }
        enqueueWork()

        return workState.collectUntilDone().onLoaded {
            markAsComplete()
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
