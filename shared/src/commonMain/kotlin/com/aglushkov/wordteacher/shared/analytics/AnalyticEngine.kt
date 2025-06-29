package com.aglushkov.wordteacher.shared.analytics

enum class AnalyticEngineType {
    AppMetrica
}

data class AnalyticEvent(
    val name: String,
    val params: Map<String, Any>,
    val engineType: AnalyticEngineType,
    val throwable: Throwable? = null
) {
    companion object {
        fun createScreenEvent(name: String): AnalyticEvent {
            return AnalyticEvent(
                name = name,
                params = mapOf(),
                engineType = AnalyticEngineType.AppMetrica
            )
        }
        fun createActionEvent(name: String, params: Map<String, Any?> = mapOf()): AnalyticEvent {
            return AnalyticEvent(
                name = name,
                params = params.filterValues { it != null } as Map<String, Any>,
                engineType = AnalyticEngineType.AppMetrica
            )
        }
        fun createErrorEvent(message: String, throwable: Throwable): AnalyticEvent {
            return AnalyticEvent(
                name = message,
                params = mapOf(),
                engineType = AnalyticEngineType.AppMetrica,
                throwable = throwable,
            )
        }
    }
}

interface AnalyticEngine {
    val type: AnalyticEngineType

    fun send(e: AnalyticEvent)
}
