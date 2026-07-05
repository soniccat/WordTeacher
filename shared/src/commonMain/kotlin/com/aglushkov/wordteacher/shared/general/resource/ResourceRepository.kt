package com.aglushkov.wordteacher.shared.general.resource

import com.aglushkov.wordteacher.shared.general.extensions.takeUntilLoadedOrErrorForVersion
import com.aglushkov.wordteacher.shared.general.extensions.updateWithLoadingData
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilDone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ResourceRepository<T, A> {
//    val scope: CoroutineScope
    val value: Resource<T>
    val stateFlow: StateFlow<Resource<T>>

    suspend fun loadIfNotLoaded(arg: A, initialValue: Resource<T> = stateFlow.value): Flow<Resource<T>>
    fun launchLoadInScope(arg: A, initialValue: Resource<T> = stateFlow.value, launchScope: CoroutineScope)
    fun loadInScope(
        arg: A,
        initialValue: Resource<T> = stateFlow.value,
        launchScope: CoroutineScope,
    ): Flow<Resource<T>> {
        launchLoadInScope(arg, initialValue, launchScope)
        return stateFlow.takeUntilLoadedOrErrorForVersion()
    }

    suspend fun load(arg: A, initialValue: Resource<T> = stateFlow.value): Flow<Resource<T>> =
        coroutineScope {
            loadInScope(arg, initialValue, this)
        }

    suspend fun launchLoad(arg: A, initialValue: Resource<T> = stateFlow.value) =
        coroutineScope {
            launchLoadInScope(arg, initialValue, this)
        }
}

abstract class SimpleResourceRepository<T, A>(
    initialValue: Resource<T> = Resource.Uninitialized(),
//    override val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val canTryAgain: Boolean = true,
    private val needPreload: Boolean = false,
): ResourceRepository<T, A> {
    override val value: Resource<T>
        get() = stateFlow.value
    override val stateFlow = MutableStateFlow(initialValue)
    private var loadJob: Job? = null
    private val loadIfNotLoadedMutex = Mutex()

    override suspend fun loadIfNotLoaded(arg: A, initialValue: Resource<T>): Flow<Resource<T>> {
        return loadIfNotLoadedMutex.withLock {
            if (value.isNotLoadedAndNotLoading()) {
                load(arg, initialValue)
            } else {
                stateFlow.takeUntilLoadedOrErrorForVersion()
            }
        }
    }

    override fun launchLoadInScope(
        arg: A,
        initialValue: Resource<T>,
        launchScope: CoroutineScope,
    ) {
        loadJob?.cancel()

        // Keep version for Uninitialized to support flow collecting in advance when services aren't loaded
        val bumpedValue = if (initialValue.isLoaded()) {
            initialValue.bumpVersion()
        } else {
            initialValue
        }

        val resultNeedPreload = stateFlow.value.isUninitialized() && needPreload
        stateFlow.update { bumpedValue.toLoading() }
        loadJob = launchScope.launch {
            if (resultNeedPreload) {
                loadResource { preload(arg) }
                    .waitUntilDone()
                    .onData(stateFlow::updateWithLoadingData)
            }

            handleLoading(arg)
        }
    }

    protected open suspend fun handleLoading(arg: A) {
        loadResource(
            initialValue = stateFlow.value,
            canTryAgain = canTryAgain,
            loader = { loadInternal(arg) },
        ).collect(stateFlow)
    }
    protected open suspend fun preload(arg: A): T? = null
    protected abstract suspend fun loadInternal(arg: A): T

    fun clear() {
        stateFlow.update { Resource.Uninitialized() }
    }
}

fun <T, A> buildSimpleResourceRepository(
//    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    preload: (suspend (arg: A) -> T?)? = null,
    load: suspend (arg: A) -> T
): SimpleResourceRepository<T, A> {
    return object : SimpleResourceRepository<T,A>(
//        scope = scope,
    ) {
        override suspend fun preload(arg: A): T? {
            return preload?.invoke(arg)
        }

        override suspend fun loadInternal(arg: A): T {
            return load(arg)
        }
    }
}
