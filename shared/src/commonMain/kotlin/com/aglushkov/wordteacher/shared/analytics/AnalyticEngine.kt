package com.aglushkov.wordteacher.shared.analytics

enum class AnalyticEngineType {
    AppMetrica
}

data class AnalyticEvent(
    val engineType: AnalyticEngineType,
    val params: Map<String, Any>
)

interface AnalyticEngine {
    val type: AnalyticEngineType

    fun send(e: AnalyticEvent)
}
