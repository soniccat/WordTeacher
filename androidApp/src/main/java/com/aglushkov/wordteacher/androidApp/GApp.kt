package com.aglushkov.wordteacher.androidApp

import android.app.Application
import com.aglushkov.wordteacher.androidApp.general.ActivityVisibilityResolver
import com.aglushkov.wordteacher.di.AppComponent
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.di.DaggerAppComponent
import com.aglushkov.wordteacher.di.GeneralModule
import com.aglushkov.wordteacher.shared.general.Logger


class GApp: Application(), AppComponentOwner, ActivityVisibilityResolver.Listener {
    private lateinit var activityVisibilityResolver: ActivityVisibilityResolver
    override lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()

        Logger().setupDebug()

        appComponent = DaggerAppComponent.builder().generalModule(GeneralModule(this)).build()
        appComponent.connectivityManager().checkNetworkState()

        initActivityVisibilityResolver()
    }

    private fun initActivityVisibilityResolver() {
        activityVisibilityResolver = ActivityVisibilityResolver(this)
        activityVisibilityResolver.listener = this
        activityVisibilityResolver.attach()
    }

    override fun onFirstActivityStarted() {
        appComponent.connectivityManager().register()
    }

    override fun onLastActivityStopped() {
        appComponent.connectivityManager().unregister()
    }
}