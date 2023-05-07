package com.aglushkov.wordteacher.android_app.di

import android.app.Application
import android.content.Context
import com.aglushkov.wordteacher.shared.di.AppComp
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import dagger.Module
import dagger.Provides


@Module
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
}