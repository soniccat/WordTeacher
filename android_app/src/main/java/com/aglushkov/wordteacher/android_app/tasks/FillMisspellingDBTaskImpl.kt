package com.aglushkov.wordteacher.android_app.tasks

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.impl.WorkManagerImpl
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_CLASS_NAME
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME
import androidx.work.multiprocess.RemoteWorkerService
import com.aglushkov.wordteacher.shared.general.resource.onError
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.repository.suggestion.SymSpellRepository
import com.aglushkov.wordteacher.shared.tasks.FillMisspellingDBTask
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import java.util.concurrent.TimeUnit

class FillMisspellingDBTaskImpl(
    private val context: Context,
    private val settings: SettingStore,
    private val lastVersion: Int,
): FillMisspellingDBTask(settings, lastVersion) {
    override suspend fun process() {
        val workManager = WorkManager.getInstance(context)

        val PACKAGE_NAME = "com.aglushkov.wordteacher.multiprocess"
        val serviceName = RemoteWorkerService::class.java.name
        val componentName = ComponentName(PACKAGE_NAME, serviceName)

        val data: Data = Data.Builder()
            .putString(ARGUMENT_PACKAGE_NAME, componentName.packageName)
            .putString(ARGUMENT_CLASS_NAME, componentName.className)
            .build()
        val continuation = workManager.beginUniqueWork(
            FILL_MISSPELLING_DB_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<FillMisspellingDBWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .setRequiresDeviceIdle(true)
                    .build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
                .setInputData(data)
                .build()
        )
        val operation = continuation.enqueue()

        workManager.getWorkInfosForUniqueWorkFlow(FILL_MISSPELLING_DB_WORK).collect {
            if (it.isNotEmpty()) {
                if (it.first().state == WorkInfo.State.SUCCEEDED) {
                    markAsComplete()
                }
            }
        }
    }
}

class FillMisspellingDBWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val symSpellRepository: SymSpellRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            symSpellRepository.load(Unit).collect()
        } catch (e: Throwable) {
            if (e is CancellationException) {
                return Result.retry()
            }
            return Result.failure()
        }

        symSpellRepository.value.onError {
            if (it is CancellationException) {
                return Result.retry()
            }
            return Result.failure()
        }

        return Result.success()
    }

    @AssistedFactory
    interface Factory : CustomWorkerFactory {
        override fun create(
            @Assisted context: Context,
            @Assisted params: WorkerParameters
        ): FillMisspellingDBWorker
    }
}

interface CustomWorkerFactory {
    fun create(context: Context, params: WorkerParameters): ListenableWorker
}

private const val FILL_MISSPELLING_DB_WORK = "FILL_MISSPELLING_DB_WORK"