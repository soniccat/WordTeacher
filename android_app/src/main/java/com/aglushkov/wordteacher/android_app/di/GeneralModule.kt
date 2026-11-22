package com.aglushkov.wordteacher.android_app.di

import android.app.Application
import android.content.Context
import com.aglushkov.wordteacher.android_app.BuildConfig
import com.aglushkov.wordteacher.android_app.repository.NotificationPermissionRepository
import com.aglushkov.wordteacher.shared.di.AppComp
import com.aglushkov.wordteacher.shared.di.IsDebug
import com.aglushkov.wordteacher.shared.di.SharedAppModule
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import dagger.Module
import dagger.Provides


@Module(includes = [SharedAppModule::class, CustomWorkerModule::class])
class GeneralModule(private val application: Application) {
    @Provides
    fun application(): Application {
        return application
    }

    @Provides
    fun context(): Context {
        return application
    }

    @AppComp
    @Provides
    fun connectivityManager(context: Context): ConnectivityManager {
        return ConnectivityManager(context)
    }

    @IsDebug
    @AppComp
    @Provides
    fun isDebug(): Boolean = false// BuildConfig.DEBUG

    @AppComp
    @Provides
    fun notificationPermissionRepository(context: Context): NotificationPermissionRepository {
        return NotificationPermissionRepository(context)
    }
}