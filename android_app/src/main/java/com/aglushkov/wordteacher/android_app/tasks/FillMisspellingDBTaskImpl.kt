package com.aglushkov.wordteacher.android_app.tasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.impl.foreground.SystemForegroundService
import androidx.work.impl.utils.ForceStopRunnable
import androidx.work.multiprocess.RemoteCoroutineWorker
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_CLASS_NAME
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME
import androidx.work.multiprocess.RemoteWorkerService
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.tasks.FillMisspellingDBTask
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import kotlin.time.measureTime

class FillMisspellingDBTaskImpl(
    private val context: Context,
    private val settings: SettingStore,
    private val lastVersion: Int,
    private val analytics: Analytics,
): FillMisspellingDBTask(settings, lastVersion, analytics) {
    override suspend fun process() {
        val workManager = WorkManager.getInstance(context)
        val componentName = ComponentName(context.packageName, RemoteWorkerService::class.java.name)

        val data: Data = Data.Builder()
            .putString(ARGUMENT_PACKAGE_NAME, componentName.packageName)
            .putString(ARGUMENT_CLASS_NAME, componentName.className)
            .build()
        val continuation = workManager.beginUniqueWork(
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
//    private val symSpellRepository: SymSpellRepository,
) : RemoteCoroutineWorker(context, params) {

    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.US)
    val logger = java.util.logging.Logger.getLogger("fillingMisspellingDB")

    override suspend fun doRemoteWork(): Result {
        setForegroundAsync(createForegroundInfo("Start text"))

        try {
            val currentDateTime = LocalDateTime.now()
            val formattedDate: String = currentDateTime.format(formatter)

            Logger.e("$formattedDate: start", "FillMisspellingDBWorker")
            val d = measureTime {
                while(true) {
                    delay(100)
                    logger.log(Level.WARNING, "in worker")
                    if (isStopped) {
                        break
                    }
                }
            }
            Logger.e("in worker: ${d.inWholeMilliseconds}", "FillMisspellingDBWorker")
//            try {
//                symSpellRepository.load(Unit).collect()
//            } catch (e: Throwable) {
//                if (e is CancellationException) {
//                    return Result.retry()
//                }
//                return Result.failure()
//            }
//
//            symSpellRepository.value.onError {
//                if (it is CancellationException) {
//                    return Result.retry()
//                }
//                return Result.failure()
//            }

            return Result.retry()
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Logger.e("stopReason: $stopReason", "FillMisspellingDBWorker")
            }
        }
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val title = applicationContext.getString(R.string.misspelling_notification_title)
        val cancel = applicationContext.getString(R.string.misspelling_notification_cancel)

        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        // Create the NotificationChannel.
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(FILL_MISSPELLING_CHANNEL_ID, title, importance)
        mChannel.description = applicationContext.getString(R.string.misspelling_notification_description)

        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        val notificationManager =
            applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(mChannel)

        val notification = NotificationCompat.Builder(applicationContext, FILL_MISSPELLING_CHANNEL_ID)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_error_24)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
//            .addAction(R.drawable.ic_error_24, "cancel", intent)
//            .addAction(0, cancel,
//                PendingIntent.getBroadcast(
//                    applicationContext,
//                    100,
//                    Intent(applicationContext, NotificationActionReceiver::class.java),
//                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
//                )
//            )
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                FILL_MISSPELLING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(
                FILL_MISSPELLING_NOTIFICATION_ID,
                notification,
            )
        }
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
private const val FILL_MISSPELLING_NOTIFICATION_ID = 1000
private const val FILL_MISSPELLING_CHANNEL_ID = "FILL_MISSPELLING_DB"