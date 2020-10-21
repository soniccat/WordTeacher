package com.aglushkov.wordteacher.di

import android.content.Context
import com.aglushkov.wordteacher.androidApp.di.AppComp
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import dagger.Module
import dagger.Provides


@Module
class GeneralModule(private val aContext: Context) {
    @Provides
    fun context(): Context {
        return aContext
    }

    @AppComp
    @Provides
    fun connectivityManager(context: Context): ConnectivityManager {
        return ConnectivityManager(context)
    }
}