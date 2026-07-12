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
import com.aglushkov.wordteacher.shared.general.extensions.collectUntilDone
import com.aglushkov.wordteacher.shared.general.resource.loadResourceWithProgress
import com.aglushkov.wordteacher.shared.general.resource.onError
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.repository.suggestion.SymSpellRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collect
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.measureTime

class FillMisspellingDBWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
    private val symSpellRepository: SymSpellRepository,
) : CoroutineWorker(context, params) {

    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.US)

    override suspend fun doWork(): Result {
        var isForegroundServiceAvailable = false
        try {
            if (isAppInForeground(context)) {
                isForegroundServiceAvailable = true
                setForeground(createForegroundInfo(0.0f))
            }
        } catch (e: Throwable) {
            return Result.failure()
        }

        try {
            val currentDateTime = LocalDateTime.now()
            val formattedDate: String = currentDateTime.format(formatter)

            Logger.e("$formattedDate: start", "FillMisspellingDBWorker")
            val d = measureTime {
                try {
                    loadResourceWithProgress(
                        loader = symSpellRepository.load()
                    ).collect {
                        if (isForegroundServiceAvailable) {
                            setForeground(createForegroundInfo(it.progress()))
                        }
                    }
                } catch (e: Throwable) {
                    if (e is CancellationException) {
                        return Result.retry()
                    }
                    return Result.failure()
                }
            }
            Logger.e("in worker: ${d.inWholeMilliseconds}", "FillMisspellingDBWorker")
            return Result.success()
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Logger.e("stopReason: $stopReason", "FillMisspellingDBWorker")
            }
        }
    }

    private fun createForegroundInfo(progress: Float): ForegroundInfo {
        val title = applicationContext.getString(R.string.misspelling_notification_title)
        val cancel = applicationContext.getString(R.string.misspelling_notification_cancel)

        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        // Create the NotificationChannel.
        val mChannel = NotificationChannel(FILL_MISSPELLING_CHANNEL_ID, title, NotificationManager.IMPORTANCE_LOW)
        mChannel.description = applicationContext.getString(R.string.misspelling_notification_description)

        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        val notificationManager =
            applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(mChannel)

        val notification = NotificationCompat.Builder(applicationContext, FILL_MISSPELLING_CHANNEL_ID)
            .setContentTitle(title)
            .setTicker(title)
            .setDeleteIntent(intent)
            .setSilent(true)
            .setSmallIcon(R.drawable.ic_statusbar)
            .setProgress(
                100,
                (100*progress).toInt(),
                false,
            )
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(R.drawable.ic_close_18, cancel, intent)
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