package com.aglushkov.wordteacher.shared.general.resource

import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface ResourceRepository<T, A> {
    val stateFlow: StateFlow<Resource<T>>

    fun load(arg: A, initialValue: Resource<T> = stateFlow.value): StateFlow<Resource<T>>
}

abstract class SimpleResourceRepository<T, A>(
    initialValue: Resource<T> = Resource.Uninitialized(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    private val canTryAgain: Boolean = true,
): ResourceRepository<T, A> {
    override val stateFlow = MutableStateFlow(initialValue)
    private var loadJob: Job? = null

    override fun load(arg: A, initialValue: Resource<T>): StateFlow<Resource<T>> {
        loadJob?.cancel()

        // Keep version for Uninitialized to support flow collecting in advance when services aren't loaded
        val bumpedValue = if (initialValue.isLoadedOrError()) {
            initialValue.bumpVersion()
        } else {
            initialValue
        }

        loadJob = scope.launch {
            loadResource(
                initialValue = bumpedValue,
                canTryAgain = canTryAgain,
                loader = { load(arg) },
            ).collect(stateFlow)
        }

        return stateFlow
    }

    abstract suspend fun load(arg: A): T
}