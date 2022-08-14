package com.aglushkov.wordteacher.androidApp

import android.app.Application
import com.aglushkov.wordteacher.androidApp.general.ActivityVisibilityResolver
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.di.AppComponent
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.di.DaggerAppComponent
import com.aglushkov.wordteacher.di.GeneralModule
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject


class GApp: Application(), AppComponentOwner, ActivityVisibilityResolver.Listener {
    override lateinit var appComponent: AppComponent
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Inject lateinit var nlpCore: NLPCore
    @Inject lateinit var routerResolver: RouterResolver
    @Inject lateinit var activityVisibilityResolver: ActivityVisibilityResolver
    @Inject lateinit var databaseCardWorker: DatabaseCardWorker

    override fun onCreate() {
        super.onCreate()

        Logger().setupDebug()

        appComponent = DaggerAppComponent.builder()
            .generalModule(GeneralModule(this))
            .build()
        appComponent.injectApplication(this)

        routerResolver.attach()
        activityVisibilityResolver.listener = this
        activityVisibilityResolver.attach()

        appComponent.connectivityManager().checkNetworkState()

        mainScope.launch(Dispatchers.Default) {
            nlpCore.load()
        }
    }

    override fun onFirstActivityStarted() {
        appComponent.connectivityManager().register()
    }

    override fun onLastActivityStopped() {
        appComponent.connectivityManager().unregister()
    }
}