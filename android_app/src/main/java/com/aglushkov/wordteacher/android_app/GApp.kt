package com.aglushkov.wordteacher.android_app

import android.app.ActivityManager
import android.app.Application
import android.content.res.Resources
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import android.os.Process
import androidx.work.Configuration
import com.aglushkov.wordteacher.android_app.di.AppComponent
import com.aglushkov.wordteacher.android_app.di.AppComponentOwner
import com.aglushkov.wordteacher.android_app.di.DaggerAppComponent
import com.aglushkov.wordteacher.android_app.di.GeneralModule
import com.aglushkov.wordteacher.android_app.general.ActivityVisibilityResolver
import com.aglushkov.wordteacher.android_app.general.RouterResolver
import com.aglushkov.wordteacher.android_app.tasks.CompositeWorkerFactory
import com.aglushkov.wordteacher.android_app.tasks.CustomWorkerFactory
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.di.IsDebug
import com.aglushkov.wordteacher.shared.general.FileLogger
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilDone
import com.aglushkov.wordteacher.shared.general.setAnalytics
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyDatabase
import com.aglushkov.wordteacher.shared.repository.suggestion.SymSpellRepository
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
import org.telegram.login.TelegramLogin
import javax.inject.Inject

// to inject some fields for non main process
class GAppNonMainProccess {
    @Inject
    lateinit var workerFactory: CompositeWorkerFactory

}

class GApp: Application(), AppComponentOwner, ActivityVisibilityResolver.Listener, Configuration.Provider {
    override lateinit var appComponent: AppComponent
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Inject lateinit var nlpCore: NLPCore
    @Inject lateinit var routerResolver: RouterResolver
    @Inject lateinit var activityVisibilityResolver: ActivityVisibilityResolver

    @Inject
    lateinit var workerFactory: CompositeWorkerFactory

    // declare here to force initialization on startup (main process only)
    @Inject lateinit var analytics: Analytics
    @Inject lateinit var databaseCardWorker: DatabaseCardWorker
    @Inject lateinit var cookieStorage: CookiesStorage
    @Inject lateinit var freqDb: WordFrequencyDatabase
    @Inject lateinit var wordDefinitionRepository: WordDefinitionRepository
    @Inject lateinit var fileLogger: FileLogger
    @Inject lateinit var tasks: Array<Task>
    @Inject lateinit var symSpellRepository: SymSpellRepository

    private var nonMainProcessDeps: GAppNonMainProccess? = null

    override fun onCreate() {
        super.onCreate()

        if (isMainProcess()) {
            onMainProcessCreated()
        }else {
            onNonMainProcessCreated()
        }
    }

    private fun onMainProcessCreated() {
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
        TelegramLogin.init(
            clientId = resources.getString(R.string.telegram_client_id),
            redirectUri = buildString {
                append("https://")
                append(getTelegramRedirect(resources, appComponent.isDebug()))
                append("/tglogin")
            }
        )

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

    private fun onNonMainProcessCreated() {
        nonMainProcessDeps = GAppNonMainProccess().apply {
            DaggerAppComponent.builder()
                .generalModule(GeneralModule(this@GApp))
                .build()
                .injectAppNonMainProccess(this)
        }
    }

    private fun isMainProcess(): Boolean {
        return packageName == myProcessName()
    }

    private fun myProcessName(): String? {
        val mypid = Process.myPid()
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val infos = manager.runningAppProcesses
        for (info in infos) {
            if (info.pid == mypid) {
                return info.processName
            }
        }
        return null
    }

    override fun onFirstActivityStarted() {
        appComponent.connectivityManager().register()
    }

    override fun onLastActivityStopped() {
        appComponent.connectivityManager().unregister()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

fun getTelegramRedirect(res: Resources, isDebug: Boolean) =
    res.getString(if(isDebug) R.string.telegram_redirect_release else R.string.telegram_redirect_release)