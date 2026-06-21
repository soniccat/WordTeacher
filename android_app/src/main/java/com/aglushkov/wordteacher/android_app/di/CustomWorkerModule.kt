package com.aglushkov.wordteacher.android_app.di

import androidx.work.ListenableWorker
import com.aglushkov.wordteacher.android_app.tasks.CustomWorkerFactory
import com.aglushkov.wordteacher.android_app.tasks.FillMisspellingDBWorker
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

@Module
interface CustomWorkerModule {
    @Binds
    @IntoMap
    @WorkerKey(FillMisspellingDBWorker::class)
    fun bindMyCustomWorker(factory: FillMisspellingDBWorker.Factory): CustomWorkerFactory
}

@MapKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WorkerKey(val value: KClass<out ListenableWorker>)