package com.aglushkov.wordteacher.shared.general.extensions

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.v
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile

// TODO: replace with simple .collect(stateFlow)
suspend fun <T> Flow<T>.forward(stateFlow: MutableStateFlow<T>) {
    collect { newRes ->
        stateFlow.value = newRes
    }
}

suspend fun <T> StateFlow<Resource<T>>.forwardForVersion(
    stateFlow: MutableStateFlow<Resource<T>>
) = forward(value.version, stateFlow)

suspend fun <T> Flow<Resource<T>>.forward(version: Int, stateFlow: MutableStateFlow<Resource<T>>) {
    collect { newRes ->
        applyResValueIfNeeded(version, newRes) {
            stateFlow.value = newRes
        }
    }
}

suspend fun <T> StateFlow<Resource<T>>.collectUntilLoaded(): T {
    return first { it.isLoaded() }.data()!!
}

// Take until a resource operation is completed, the last state is emitted
fun <T> StateFlow<Resource<T>>.takeUntilLoadedOrErrorForVersion(
    version: Int = value.version
): Flow<Resource<T>> {
    return flow {
        try {
            // TODO: replace with takeWhile/transformWhile
            collect { newRes ->
                Logger.v("got value.version(${value.version}) with version(${version}) " + value)
                applyResValueIfNeeded(
                    startVersion = version,
                    newRes = newRes,
                    applyFun = {
                        if (newRes.isLoadedOrError()) {
                            emit(newRes)
                            throw AbortFlowException(this)
                        } else {
                            emit(newRes)
                        }
                    },
                    createOutdatedException = {
                        AbortFlowException(this@flow)
                    }
                )
            }
        } catch (e: AbortFlowException) {
            if (this != e.owner) {
                throw e
            }
        }
    }
}

private suspend fun <T> applyResValueIfNeeded(
    startVersion: Int,
    newRes: Resource<T>,
    applyFun: suspend () -> Unit
) {
    applyResValueIfNeeded(startVersion, newRes, applyFun) {
        CancellationException("Version ${startVersion} is outdated with version ${newRes.version}")
    }
}

private suspend fun <T> applyResValueIfNeeded(
    startVersion: Int,
    newRes: Resource<T>,
    applyFun: suspend () -> Unit,
    createOutdatedException: () -> CancellationException
) {
    when {
        startVersion == newRes.version -> {
            applyFun()
        }
        startVersion < newRes.version -> {
            throw createOutdatedException()
        }
        else -> {
            Logger.v("Got value from prev version: version(${startVersion}) and newRes.version(${newRes.version})")
        }
    }
}

fun <T> StateFlow<T?>.takeWhileNonNull(
    collectCurrentNull: Boolean = true
) = flow<T> {
        if (value == null && collectCurrentNull) {
            first { it != null }
        }

        takeWhile { it != null }.collect(this as FlowCollector<T?>)
    }

class AbortFlowException constructor(
    val owner: FlowCollector<*>
) : CancellationException("Flow was aborted, no more elements needed")
