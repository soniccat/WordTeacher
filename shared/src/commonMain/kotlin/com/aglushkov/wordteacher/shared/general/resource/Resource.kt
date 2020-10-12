package com.aglushkov.wordteacher.shared.general.resource

import kotlin.js.JsName


sealed class Resource<T>(needShowNext: Boolean) {
    val canLoadNextPage: Boolean = needShowNext

    class Uninitialized<T> : Resource<T>(false)
    class Loaded<T>(val data: T, canLoadNext: Boolean = false) : Resource<T>(canLoadNext)
    class Loading<T>(val data: T? = null, canLoadNext: Boolean = false) : Resource<T>(canLoadNext)
    class Error<T>(val throwable: Throwable, val canTryAgain: Boolean, val data: T? = null, canLoadNext: Boolean = false) : Resource<T>(canLoadNext)

    fun toLoading(data: T? = data(), canLoadNext: Boolean = this.canLoadNextPage) = Loading(data, canLoadNext)
    fun toLoaded(data: T, canLoadNext: Boolean = this.canLoadNextPage) = Loaded(data, canLoadNext)
    fun toError(throwable: Throwable, canTryAgain: Boolean, data: T? = data(), canLoadNext: Boolean = this.canLoadNextPage) = Error(throwable, canTryAgain, data, canLoadNext)

    // Getters
    fun isUninitialized(): Boolean {
        return when(this) {
            is Uninitialized -> true
            else -> false
        }
    }

    @JsName("resData")
    fun data(): T? {
        return when (this) {
            is Loaded -> this.data
            is Loading -> this.data
            is Error -> this.data
            else -> null
        }
    }

    fun copy(data: T? = this.data()): Resource<T> {
        return when(this) {
            is Uninitialized -> Uninitialized()
            is Loaded -> Loaded(data!!, canLoadNextPage)
            is Loading -> Loading(data, canLoadNextPage)
            is Error -> Error(throwable, canTryAgain, data, canLoadNextPage)
        }
    }

    fun <R> copyWith(data: R?): Resource<R> {
        return when(this) {
            is Uninitialized -> Uninitialized()
            is Loaded -> Loaded(data!!, canLoadNextPage)
            is Loading -> Loading(data, canLoadNextPage)
            is Error -> Error(throwable, canTryAgain, data, canLoadNextPage)
        }
    }
}


fun <T> Resource<T>?.data(): T? {
    return this?.data()
}

fun Resource<*>?.isUninitialized(): Boolean {
    return this?.isUninitialized() ?: true
}

fun Resource<*>?.isError(): Boolean {
    return if (this == null) false else this is Resource.Error
}

fun Resource<*>?.isLoaded(): Boolean {
    return if (this == null) false else this is Resource.Loaded
}

fun Resource<*>?.isLoading(): Boolean {
    return if (this == null) false else this is Resource.Loading
}

fun <T> Resource<T>?.toLoading(): Resource.Loading<T> {
    return this?.toLoading() ?: Resource.Loading()
}

fun Resource<*>?.isNotLoadedAndNotLoading(): Boolean {
    return if (this == null) false else this !is Resource.Loaded && this !is Resource.Loading
}

fun Resource<*>?.isNotLoading(): Boolean {
    return if (this == null) false else this !is Resource.Loading
}

fun Resource<*>?.hasData(): Boolean {
    return if (this == null) false else this.data() != null
}

fun <T> Resource<T>?.isLoadedAndEmpty(): Boolean where T : Collection<*> {
    return if (this == null) false else (this is Resource.Loaded && data()?.isEmpty() == true)
}

fun <T> Resource<T>?.isLoadedAndNotEmpty(): Boolean where T : Collection<*> {
    return if (this == null) false else (this is Resource.Loaded && data()?.isNotEmpty() == true)
}

fun Resource<*>?.isLoadedOrError(): Boolean {
    return if (this == null) false else (this is Resource.Loaded || this is Resource.Error)
}

fun <T, D> Resource<T>.merge(res2: Resource<D>): Resource<Pair<T?, D?>> {
    if (isUninitialized() || res2.isUninitialized()) {
        return Resource.Uninitialized()
    }

    if (isLoading() || res2.isLoading()) {
        return Resource.Loading(toPair(res2))
    }

    if (isError() || res2.isError()) {
        val throwable = getErrorThrowable(res2)
        return Resource.Error(throwable!!, getErrorTryAgain(res2), toPair(res2))
    }

    return Resource.Loaded(toPair(res2))
}

fun <D, T> Resource<T>.getErrorThrowable(res2: Resource<D>): Throwable? {
    return if (this is Resource.Error) throwable else if (res2 is Resource.Error) res2.throwable else null
}

fun <D, T> Resource<T>.getErrorTryAgain(res2: Resource<D>): Boolean {
    return if (this is Resource.Error) canTryAgain else if (res2 is Resource.Error) res2.canTryAgain else false
}

fun <D, T> Resource<T>.toPair(res2: Resource<D>) = Pair(data(), res2.data())

fun <D, T, P> Resource<T>.toTriple(res2: Resource<D>, res3: Resource<P>) = Triple(data(), res2.data(), res3.data())
