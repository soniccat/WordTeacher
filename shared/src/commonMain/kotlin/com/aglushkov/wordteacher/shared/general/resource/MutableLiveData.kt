package com.aglushkov.wordteacher.shared.general.resource

import dev.icerock.moko.mvvm.livedata.MutableLiveData
import kotlinx.coroutines.CancellationException

suspend fun <T> MutableLiveData<Resource<T>>.load(
    canTryAgain: Boolean,
    loader: suspend () -> T?
) {
    val initialValue = value
    val loadingRes = value.toLoading()
    value = loadingRes

    try {
        val result = loader()
        val newStatus: Resource<T> = if (result != null) {
            Resource.Loaded(result)
        } else {
            Resource.Uninitialized()
        }

        if (value == loadingRes) {
            postValue(newStatus)
        }
    } catch (e: CancellationException) {
        if (value == loadingRes) {
            postValue(initialValue)
        }
        throw e
    } catch (e: Exception) {
        if (value == loadingRes) {
            val errorRes = initialValue.toError(e, canTryAgain)
            postValue(errorRes)
        }
    }
}