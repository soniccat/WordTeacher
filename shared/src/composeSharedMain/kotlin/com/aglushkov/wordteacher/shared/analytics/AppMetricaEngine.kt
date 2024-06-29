package com.aglushkov.wordteacher.shared.analytics

class AppMetricaEngine(
    key: String
): AnalyticEngine {
    override val type: AnalyticEngineType = AnalyticEngineType.AppMetrica

    init {
        val config = AppMetricaConfig.newConfigBuilder(key).build()
    }

    override fun send(e: AnalyticEvent) {

    }
}