package com.aglushkov.wordteacher.shared.general.extensions

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isError
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.v
import dev.icerock.moko.mvvm.livedata.LiveData
import dev.icerock.moko.mvvm.livedata.MutableLiveData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile

suspend fun <T> Flow<T>.forward(stateFlow: MutableStateFlow<T>) {
    collect {
        stateFlow.value = it
    }
}

suspend fun <T, D> Flow<Resource<T>>.forward(
    liveData: MutableLiveData<Resource<D>>,
    transform: (T?) -> D
) {
    collect {
        val viewItems = transform(it.data())
        liveData.postValue(it.copyWith(viewItems))
    }
}

// useful for a StateFlow to collect only the current loading session states
suspend fun <T> Flow<Resource<T>>.forwardUntilLoadedOrError(
    flow: MutableStateFlow<Resource<T>>
) {
    dropWhile {
        it.isError() // skip an error from the previous loading attempt
    }.takeUntilLoadedOrError().collect { res ->
        flow.value = res
    }
}

// useful for a StateFlow to collect only the current loading session states
suspend fun <T, D> Flow<Resource<T>>.forwardUntilLoadedOrError(
    liveData: MutableLiveData<Resource<D>>,
    transform: (Resource<T>) -> D
) {
    dropWhile {
        it.isError() // skip an error from the previous loading attempt
    }.takeUntilLoadedOrError().collect { res ->
        val viewItemsRes = res.copyWith(transform(res))
        liveData.postValue(viewItemsRes)
    }
}

// Take until a resource operation is completed, the last state is emitted
private fun <T> Flow<Resource<T>>.takeUntilLoadedOrError(): Flow<Resource<T>> {
    return flow {
        try {
            collect { value ->
                Logger.v("taken " + value)
                if (value.isLoadedOrError()) {
                    emit(value)
                    throw AbortFlowException(this)
                } else {
                    emit(value)
                }
            }
        } catch (e: AbortFlowException) {
            if (this != e.owner) {
                throw e
            }
        }
    }
}

class AbortFlowException constructor(
    val owner: FlowCollector<*>
) : CancellationException("Flow was aborted, no more elements needed")