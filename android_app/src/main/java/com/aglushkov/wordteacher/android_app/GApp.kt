package com.aglushkov.wordteacher.android_app

import android.app.Application
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import com.aglushkov.wordteacher.android_app.di.AppComponent
import com.aglushkov.wordteacher.android_app.di.AppComponentOwner
import com.aglushkov.wordteacher.android_app.di.DaggerAppComponent
import com.aglushkov.wordteacher.android_app.di.GeneralModule
import com.aglushkov.wordteacher.android_app.general.ActivityVisibilityResolver
import com.aglushkov.wordteacher.android_app.general.RouterResolver
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.general.FileLogger
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.setAnalytics
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyDatabase
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.tasks.Task
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.vk.id.VKID
import io.ktor.client.plugins.cookies.CookiesStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.io.path.absolutePathString
import kotlin.io.path.outputStream


// to inject some fields for non main process
class GAppNonMainProccess {
    @Inject lateinit var analytics: Analytics
}

class GApp: Application(), AppComponentOwner, ActivityVisibilityResolver.Listener {
    override lateinit var appComponent: AppComponent
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Inject lateinit var nlpCore: NLPCore
    @Inject lateinit var routerResolver: RouterResolver
    @Inject lateinit var activityVisibilityResolver: ActivityVisibilityResolver

    // declare here to force initialization on startup
    @Inject lateinit var analytics: Analytics
    @Inject lateinit var databaseCardWorker: DatabaseCardWorker
    @Inject lateinit var cookieStorage: CookiesStorage
    @Inject lateinit var freqDb: WordFrequencyDatabase
    @Inject lateinit var wordDefinitionRepository: WordDefinitionRepository
    @Inject lateinit var fileLogger: FileLogger
    @Inject lateinit var tasks: Array<Task>

    override fun onCreate() {
        super.onCreate()

        appComponent = DaggerAppComponent.builder()
            .generalModule(GeneralModule(this))
            .build()
        appComponent.injectApplication(this)

        Logger.setAnalytics(analytics)
        Logger().setupDebug(
            StaticConfig(
                Severity.Verbose,
                buildList {
                    if (appComponent.isDebug()) {
                        add(CommonWriter())
                    }
                    add(fileLogger)
                }
            )
        )
        VKID.init(this)

        routerResolver.attach()
        activityVisibilityResolver.listener = this
        activityVisibilityResolver.attach()

        appComponent.connectivityManager().checkNetworkState()

        mainScope.launch(Dispatchers.IO) {
            val taskChannel = Channel<Task>(UNLIMITED)
            launch {
                taskChannel.receiveAsFlow().collect {
                    launch {
                        it.run(taskChannel)
                    }
                }
            }
            tasks.onEach {
                launch {
                    it.run(taskChannel)
                }
            }
        }
    }

    override fun onFirstActivityStarted() {
        appComponent.connectivityManager().register()
    }

    override fun onLastActivityStopped() {
        appComponent.connectivityManager().unregister()
    }
}