package com.aglushkov.wordteacher.android_app.worker

import android.content.ComponentName
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_CLASS_NAME
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME
import androidx.work.multiprocess.RemoteWorkerService
import com.aglushkov.wordteacher.android_app.repository.NotificationPermissionRepository
import com.aglushkov.wordteacher.android_app.tasks.FillMisspellingDBWorker
import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.general.extensions.updateWithLoadedData
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilDone
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.onLoaded
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class FillMisspellingDBController(
    private val context: Context,
    private val settings: SettingStore,
    private val lastVersion: Int,
    private val analytics: Analytics,
    private val notificationPermissionRepository: NotificationPermissionRepository,
): SimpleResourceRepository<Boolean, Unit>() {
    private val workManager = WorkManager.getInstance(context)
    private val workState = MutableStateFlow<Resource<WorkInfo>>(Resource.Uninitialized())

    init {
        val currentVersion = settings.int(MISSPELLING_FILLED_DB_VERSION_KEY, -1)
        val isFilled = currentVersion == lastVersion
        if (isFilled) {
            stateFlow.updateWithLoadedData(true)
        }

        scope.launch {
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
    }

    override suspend fun loadInternal(arg: Unit): Boolean {
        notificationPermissionRepository.loadIfNotLoaded(Unit).waitUntilDone()

        workState.update { it.toLoading() }
        enqueueWork()

        return workState.waitUntilDone().onLoaded {
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
        val componentName = ComponentName(context.packageName, RemoteWorkerService::class.java.name)
        val data: Data = Data.Builder()
            .putString(ARGUMENT_PACKAGE_NAME, componentName.packageName)
            .putString(ARGUMENT_CLASS_NAME, componentName.className)
            .build()
        workManager.beginUniqueWork(
            FILL_MISSPELLING_DB_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<FillMisspellingDBWorker>()
//                .setInitialDelay(1, TimeUnit.MINUTES)
//                .setConstraints(Constraints.Builder()
//                    .setRequiresBatteryNotLow(true)
//                    .setRequiresStorageNotLow(true)
//                    .build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
                .setInputData(data)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        ).enqueue()
    }
}

private const val FILL_MISSPELLING_DB_WORK = "FILL_MISSPELLING_DB_WORK"
private const val MISSPELLING_FILLED_DB_VERSION_KEY = "MISSPELLING_FILLED_DB_VERSION_KEY"
