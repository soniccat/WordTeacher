package com.aglushkov.wordteacher.shared.analytics

enum class AnalyticEngineType {
    AppMetrica
}

data class AnalyticEvent(
    val name: String,
    val params: Map<String, Any>,
    val engineType: AnalyticEngineType,
) {
    companion object {
        fun createScreenEvent(name: String): AnalyticEvent {
            return AnalyticEvent(
                name = name,
                params = mapOf(),
                engineType = AnalyticEngineType.AppMetrica
            )
        }
    }
}

interface AnalyticEngine {
    val type: AnalyticEngineType

    fun send(e: AnalyticEvent)
}
