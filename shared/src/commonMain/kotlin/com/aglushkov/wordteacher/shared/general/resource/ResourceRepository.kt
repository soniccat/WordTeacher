package com.aglushkov.wordteacher.shared.general.resource

import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface ResourceRepository<T> {
    val stateFlow: StateFlow<Resource<T>>

    fun load(initialValue: Resource<T> = stateFlow.value): StateFlow<Resource<T>>
}

class SimpleResourceRepository<T>(
    initialValue: Resource<T> = Resource.Uninitialized(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    private val canTryAgain: Boolean = true,
    private val loader: suspend () -> T
): ResourceRepository<T> {
    override val stateFlow = MutableStateFlow(initialValue)
    private var loadJob: Job? = null

    override fun load(initialValue: Resource<T>): StateFlow<Resource<T>> {
        loadJob?.cancel()
        loadJob = scope.launch {
            loadResource(
                initialValue = initialValue,
                canTryAgain = canTryAgain,
                loader = loader,
            ).collect(stateFlow)
        }
        return stateFlow
    }
}
