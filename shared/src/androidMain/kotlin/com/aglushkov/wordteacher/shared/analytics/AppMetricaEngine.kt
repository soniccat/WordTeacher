package com.aglushkov.wordteacher.shared.analytics

import android.app.Application
import com.aglushkov.wordteacher.shared.general.resource.onData
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import io.appmetrica.analytics.AppMetricaConfig
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.profile.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppMetricaEngine(
    key: String,
    app: Application,
    spaceAuthRepository: SpaceAuthRepository,
): AnalyticEngine {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    override val type: AnalyticEngineType = AnalyticEngineType.AppMetrica

    init {
        val config = AppMetricaConfig.newConfigBuilder(key).build()
        AppMetrica.activate(app, config)
        AppMetrica.enableActivityAutoTracking(app)
        AppMetrica.setLocationTracking(true)

        scope.launch {
            spaceAuthRepository.authDataFlow.collect { authDataRes ->
                authDataRes.onData { authData ->
                    AppMetrica.setUserProfileID(authData.user.id)
                }
            }
        }
    }

    override fun send(e: AnalyticEvent) {
        e.throwable?.let { throwable ->
            AppMetrica.reportError(e.name, throwable)
        } ?: run {
            AppMetrica.reportEvent(e.name, e.params)
        }
    }
}