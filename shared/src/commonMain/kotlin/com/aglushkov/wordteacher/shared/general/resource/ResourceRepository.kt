package com.aglushkov.wordteacher.shared.general.resource

import com.aglushkov.wordteacher.shared.general.extensions.takeUntilLoadedOrErrorForVersion
import com.aglushkov.wordteacher.shared.general.extensions.updateWithLoadedData
import com.aglushkov.wordteacher.shared.general.extensions.updateWithLoadingData
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilDone
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
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val canTryAgain: Boolean = true,
    private val needPreload: Boolean = false,
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
            if (stateFlow.value.isUninitialized() && needPreload) {
                loadResource { preload(arg) }
                    .waitUntilDone {
                        stateFlow.updateWithLoadingData(it)
                    }
            }

            loadResource(
                initialValue = stateFlow.value,
                canTryAgain = canTryAgain,
                loader = { load(arg) },
            ).collect(stateFlow)
        }

        return stateFlow.takeUntilLoadedOrErrorForVersion()
    }

    protected open suspend fun preload(arg: A): T? = null
    protected abstract suspend fun load(arg: A): T

    fun clear() {
        stateFlow.update { Resource.Uninitialized() }
    }
}

fun <T, A> buildSimpleResourceRepository(
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    preload: (suspend (arg: A) -> T?)? = null,
    load: suspend (arg: A) -> T
): SimpleResourceRepository<T, A> {
    return object : SimpleResourceRepository<T,A>(
        scope = scope,
    ) {
        override suspend fun preload(arg: A): T? {
            return preload?.invoke(arg)
        }

        override suspend fun load(arg: A): T {
            return load(arg)
        }
    }
}
