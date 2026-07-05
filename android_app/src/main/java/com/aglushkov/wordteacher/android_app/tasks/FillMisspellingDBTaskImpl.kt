package com.aglushkov.wordteacher.android_app.tasks

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilDone
import com.aglushkov.wordteacher.shared.general.resource.onError
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.repository.suggestion.SymSpellRepository
import com.aglushkov.wordteacher.shared.tasks.FillMisspellingDBTask
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.measureTime

class FillMisspellingDBTaskImpl(
    private val context: Context,
    private val settings: SettingStore,
    private val lastVersion: Int,
    private val analytics: Analytics,
): FillMisspellingDBTask(settings, lastVersion, analytics) {
    override suspend fun process() {

    }
}

class FillMisspellingDBWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
    private val symSpellRepository: SymSpellRepository, // TODO: get rid of this deps in favour of manual parsing
) : CoroutineWorker(context, params) {

    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.US)
//    val logger = java.util.logging.Logger.getLogger("fillingMisspellingDB")

    override suspend fun doWork(): Result {
//        delay(10000)
        try {
            if (isAppInForeground(context)) {
                setForeground(createForegroundInfo("Start text"))
            }
        } catch (e: Throwable) {
            return Result.failure()
        }

        try {
            val currentDateTime = LocalDateTime.now()
            val formattedDate: String = currentDateTime.format(formatter)

            Logger.e("$formattedDate: start", "FillMisspellingDBWorker")
            val d = measureTime {
//                while(true) {
//                    delay(100)
//                    logger.log(Level.WARNING, "in worker")
//                    if (isStopped) {
//                        break
//                    }
//                }

                try {
                    symSpellRepository.load(Unit).waitUntilDone()
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

            }
            Logger.e("in worker: ${d.inWholeMilliseconds}", "FillMisspellingDBWorker")

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
            .setDeleteIntent(intent)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(R.drawable.ic_error_24, "cancel", intent)
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

fun isAppInForeground(context: Context): Boolean {
    val am = context.getSystemService (Context.ACTIVITY_SERVICE) as ActivityManager
    val appProcess = am.runningAppProcesses.firstOrNull { it.processName == context.packageName }
    return appProcess?.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
}

private const val FILL_MISSPELLING_NOTIFICATION_ID = 1000
private const val FILL_MISSPELLING_CHANNEL_ID = "FILL_MISSPELLING_DB"