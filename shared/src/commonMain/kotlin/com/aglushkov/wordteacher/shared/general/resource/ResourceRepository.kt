package com.aglushkov.wordteacher.shared.general.resource

import com.aglushkov.wordteacher.shared.general.extensions.takeUntilLoadedOrErrorForVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface ResourceRepository<T, A> {
    val value: Resource<T>
    val stateFlow: StateFlow<Resource<T>>

    fun load(arg: A, initialValue: Resource<T> = stateFlow.value): Flow<Resource<T>>
}

abstract class SimpleResourceRepository<T, A>(
    initialValue: Resource<T> = Resource.Uninitialized(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    private val canTryAgain: Boolean = true,
    private val loadDelay: Long = 0
): ResourceRepository<T, A> {
    override val value: Resource<T>
        get() = stateFlow.value
    override val stateFlow = MutableStateFlow(initialValue)
    private var loadJob: Job? = null

    override fun load(arg: A, initialValue: Resource<T>): Flow<Resource<T>> {
        loadJob?.cancel()

        // Keep version for Uninitialized to support flow collecting in advance when services aren't loaded
        val bumpedValue = if (initialValue.isLoaded()) {
            initialValue.bumpVersion()
        } else {
            initialValue
        }

        stateFlow.update { bumpedValue.toLoading() }
        loadJob = scope.launch {
            if (loadDelay != 0L) {
                delay(loadDelay)
            }
            loadResource(
                initialValue = stateFlow.value,
                canTryAgain = canTryAgain,
                loader = { load(arg) },
            ).collect(stateFlow)
        }

        return stateFlow.takeUntilLoadedOrErrorForVersion()
    }

    protected abstract suspend fun load(arg: A): T

    fun clear() {
        stateFlow.update { Resource.Uninitialized() }
    }
}

fun <T, A> buildSimpleResourceRepository(
    loadDelay: Long = 0L,
    load: suspend (arg: A) -> T
): SimpleResourceRepository<T, A> {
    return object : SimpleResourceRepository<T,A>(loadDelay = loadDelay) {
        override suspend fun load(arg: A): T {
            return load(arg)
        }
    }
}
