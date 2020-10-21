package com.aglushkov.wordteacher.shared.general.resource

import dev.icerock.moko.mvvm.livedata.MutableLiveData
import kotlinx.coroutines.CancellationException

suspend fun <T> MutableLiveData<Resource<T>>.load(
    canTryAgain: Boolean = true,
    loader: suspend () -> T?
) {
    val initialValue = value
    val loadingRes = value.toLoading()
    value = loadingRes

    try {
        val result = loader()
        val newStatus: Resource<T> = if (result != null) {
            initialValue.toLoaded(result)
        } else {
            // treat a null response as Uninitialized
            Resource.Uninitialized()
        }

        if (value == loadingRes) {
            postValue(newStatus)
        }
    } catch (e: CancellationException) {
        if (value == loadingRes) {
            // show an error as it's a strange situation when we cancel the current loading and
            // don't start a new loading
            val errorRes = initialValue.toError(e, canTryAgain)
            postValue(errorRes)
        }
        throw e
    } catch (e: Exception) {
        if (value == loadingRes) {
            val errorRes = initialValue.toError(e, canTryAgain)
            postValue(errorRes)
        }
    }
}