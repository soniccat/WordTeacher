package com.aglushkov.wordteacher.shared.analytics

import android.app.Application
import io.appmetrica.analytics.AppMetricaConfig
import io.appmetrica.analytics.AppMetrica

class AppMetricaEngine(
    key: String,
    app: Application,
): AnalyticEngine {
    override val type: AnalyticEngineType = AnalyticEngineType.AppMetrica

    init {
        val config = AppMetricaConfig.newConfigBuilder(key).build()
        AppMetrica.activate(app, config)
        AppMetrica.enableActivityAutoTracking(app)
    }

    override fun send(e: AnalyticEvent) {
        AppMetrica.reportEvent(e.name, e.params)
    }
}