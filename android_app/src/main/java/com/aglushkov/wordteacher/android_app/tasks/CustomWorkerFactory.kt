package com.aglushkov.wordteacher.android_app.tasks

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject
import javax.inject.Provider

class CompositeWorkerFactory @Inject constructor(
    private val workers: Map<Class<out ListenableWorker>, @JvmSuppressWildcards Provider<CustomWorkerFactory>>
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        val entry = workers.entries.find { Class.forName(workerClassName).isAssignableFrom(it.key) }
        val factoryProvider = entry?.value ?: return null
        return factoryProvider.get().create(appContext, workerParameters)
    }
}