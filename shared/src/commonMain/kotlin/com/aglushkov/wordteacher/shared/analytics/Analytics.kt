package com.aglushkov.wordteacher.shared.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class Analytics(
    private val engines: List<AnalyticEngine>
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun sendScreen(name: String) {
        send(AnalyticEvent.createScreenEvent(name))
    }

    fun send(e: AnalyticEvent) {
        scope.launch {
            engines.firstOrNull { engine ->
                engine.type == e.engineType
            }?.send(e)
        }
    }
}
