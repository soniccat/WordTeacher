package com.aglushkov.wordteacher.shared.general.extensions

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.v
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*

// Take until a resource operation is completed, the last state is emitted
fun <T> StateFlow<Resource<T>>.takeUntilLoadedOrErrorForVersion(
    version: Int = value.version
): Flow<Resource<T>> {
    return flow {
        try {
            // TODO: replace with transformWhile if possible
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

suspend fun Flow<Boolean>.collectUntilFalse() {
    takeWhile { it }.collect()
}

suspend fun Flow<Boolean>.collectUntilTrue() {
    takeWhile { !it }.collect()
}

suspend fun <T> Flow<Resource<T>>.collectUntilLoaded(): Resource<T> {
    var res: Resource<T> = Resource.Uninitialized()
    takeWhile {
        val needTake = !it.isLoaded()
        if (!needTake) {
            res = it
        }
        needTake
    }.collect()
    return res
}

suspend fun <T> Flow<Resource<T>>.collectUntilDone(): Resource<T> {
    var res: Resource<T> = Resource.Uninitialized()
    takeWhile {
        val needTake = !it.isLoadedOrError()
        if (!needTake) {
            res = it
        }
        needTake
    }.collect()
    return res
}

suspend fun <T> Flow<Resource<T>>.collectUntilDone(
    error: suspend (Throwable) -> Unit = {},
    loaded: suspend (T) -> Unit,
) {
    val res = collectUntilDone()
    if (res is Resource.Loaded) {
        try {
            loaded(res.data)
        } catch (t: Throwable) {
            error(t)
        }
    } else if (res is Resource.Error) {
        error(res.throwable)
    }
}

fun <T> MutableStateFlow<Resource<T>>.updateLoadedData(
    defaultData: T? = null,
    dataTransform: (T) -> T
) {
    this.update {
        it.mapLoadedData(
            defaultData = defaultData,
            loadedDataTransformer = dataTransform
        )
    }
}

fun <T> MutableStateFlow<Resource<T>>.updateWithLoadedData(
    data: T
) {
    this.update { Resource.Loaded(data) }
}

fun <T> MutableStateFlow<Resource<T>>.updateWithLoadingData(
    data: T?
) {
    this.update { Resource.Loading(data) }
}

class AbortFlowException(
    val owner: FlowCollector<*>
) : CancellationException("Flow was aborted, no more elements needed")
