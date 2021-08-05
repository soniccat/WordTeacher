package com.aglushkov.wordteacher.desktopapp.di

import com.aglushkov.wordteacher.desktopapp.di.AppComp
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import dagger.Provides
import dagger.Module

@Module
class GeneralModule {

    @AppComp
    @Provides
    fun connectivityManager(): ConnectivityManager {
        return ConnectivityManager()
    }
}