package com.aglushkov.wordteacher.shared.analytics

import android.app.Application
import com.aglushkov.wordteacher.shared.general.resource.onData
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.repository.toggles.ToggleRepository
import io.appmetrica.analytics.AppMetricaConfig
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.profile.Attribute
import io.appmetrica.analytics.profile.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppMetricaEngine(
    key: String,
    app: Application,
    toggleRepositoryProvider: () -> ToggleRepository,
    spaceAuthRepository: SpaceAuthRepository,
): AnalyticEngine {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override val type: AnalyticEngineType = AnalyticEngineType.AppMetrica

    init {
        val config = AppMetricaConfig.newConfigBuilder(key).build()
        AppMetrica.activate(app, config)
        AppMetrica.enableActivityAutoTracking(app)
        AppMetrica.setLocationTracking(true)

        scope.launch {
            val togglesAsString = toggleRepositoryProvider().togglesAsString
            AppMetrica.putAppEnvironmentValue(
                "toggles", togglesAsString
            )

            spaceAuthRepository.authDataFlow.collect { authDataRes ->
                authDataRes.onData { authData ->
                    AppMetrica.setUserProfileID(authData.user.id)

                    val userProfile = UserProfile.newBuilder()
                        .apply(Attribute.customString("toggles").withValue(togglesAsString))
                        .build()
                    AppMetrica.reportUserProfile(userProfile)
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