package com.aglushkov.wordteacher.shared.general.extensions

import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isError
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import dev.icerock.moko.mvvm.livedata.LiveData
import dev.icerock.moko.mvvm.livedata.MutableLiveData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.takeWhile

suspend fun <T> Flow<T>.forward(stateFlow: MutableStateFlow<T>) {
    collect {
        stateFlow.value = it
    }
}

suspend fun <T, D> Flow<Resource<T>>.forwardUntilLoadedOrError(
    liveData: MutableLiveData<Resource<D>>,
    transform: (Resource<T>) -> D
) {
    dropWhile {
        it.isUninitialized()
    }.onStart {
        emit(Resource.Loading())
    }.catch { e ->
        emit(Resource.Error(e, true))
    }.map {
        val viewItems = transform(it)
        it.copyWith(viewItems)
    }.collect { viewItemsRes ->
        liveData.postValue(viewItemsRes)

        when (viewItemsRes) {
            is Resource.Error -> throw CancellationException(
                viewItemsRes.throwable.message,
                viewItemsRes.throwable
            )
            is Resource.Loaded -> throw CancellationException("Resource Loaded")
        }
    }
}

fun <T> Flow<Resource<T>>.takeUntilLoadedOrError(): Flow<T> = flow {
    takeWhile {
        !it.isLoaded() && !it.isError()
    }
}