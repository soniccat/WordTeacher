package com.aglushkov.wordteacher.shared.general.resource

import kotlinx.coroutines.CancellationException
import kotlin.js.JsName


sealed class Resource<T>(
    val canLoadNextPage: Boolean,
    val version: Int = 0
) {

    class Uninitialized<T>(version: Int = 0) : Resource<T>(false, version = version)
    class Loaded<T>(val data: T, canLoadNext: Boolean = false, version: Int = 0) : Resource<T>(canLoadNext, version)
    class Loading<T>(val data: T? = null, canLoadNext: Boolean = false, version: Int = 0) : Resource<T>(canLoadNext, version)
    class Error<T>(val throwable: Throwable, val canTryAgain: Boolean, val data: T? = null, canLoadNext: Boolean = false, version: Int = 0) : Resource<T>(canLoadNext, version)

    fun toLoading(data: T? = data(), canLoadNext: Boolean = this.canLoadNextPage, version: Int = this.version) = Loading(data, canLoadNext, version)
    fun toLoaded(data: T, canLoadNext: Boolean = this.canLoadNextPage, version: Int = this.version) = Loaded(data, canLoadNext, version)
    fun toError(throwable: Throwable, canTryAgain: Boolean, data: T? = data(), canLoadNext: Boolean = this.canLoadNextPage, version: Int = this.version) = Error(throwable, canTryAgain, data, canLoadNext, version)

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

    fun copy(data: T? = this.data(), version: Int = this.version): Resource<T> {
        return when(this) {
            is Uninitialized -> Uninitialized(version)
            is Loaded -> Loaded(data!!, canLoadNextPage, version)
            is Loading -> Loading(data, canLoadNextPage, version)
            is Error -> Error(throwable, canTryAgain, data, canLoadNextPage, version)
        }
    }

    fun <R> copyWith(data: R?): Resource<R> {
        return when(this) {
            is Uninitialized -> Uninitialized(version)
            is Loaded -> Loaded(data!!, canLoadNextPage, version)
            is Loading -> Loading(data, canLoadNextPage, version)
            is Error -> Error(throwable, canTryAgain, data, canLoadNextPage, version)
        }
    }

    fun bumpVersion() = copy(version = this.version + 1)
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

fun <T, D> Resource<T>.merge(res2: Resource<D>): Resource<Pair<T?, D?>> =
    merge(res2, transform = { a, b -> a to b })

fun <T, D, O> Resource<T>.merge(res2: Resource<D>, transform: (T?, D?) -> O?): Resource<O> {
    if (isUninitialized() || res2.isUninitialized()) {
        return Resource.Uninitialized()
    }

    if (isLoading() || res2.isLoading()) {
        return Resource.Loading(transform(data(), res2.data()))
    }

    if (isError() || res2.isError()) {
        val throwable = getErrorThrowable(res2)
        return Resource.Error(throwable!!, getErrorTryAgain(res2), transform(data(), res2.data()))
    }

    return Resource.Loaded(transform(data()!!, res2.data()!!)!!)
}

fun <D, T> Resource<T>.getErrorThrowable(res2: Resource<D>): Throwable? {
    return if (this is Resource.Error) throwable else if (res2 is Resource.Error) res2.throwable else null
}

fun <D, T> Resource<T>.getErrorTryAgain(res2: Resource<D>): Boolean {
    return if (this is Resource.Error) canTryAgain else if (res2 is Resource.Error) res2.canTryAgain else false
}

fun <D, T> Resource<T>.toPair(res2: Resource<D>) = Pair(data(), res2.data())

fun <D, T, P> Resource<T>.toTriple(res2: Resource<D>, res3: Resource<P>) = Triple(data(), res2.data(), res3.data())

fun <T> tryInResource(canTryAgain: Boolean = false, code: () -> T): Resource<T> {
    try {
        return Resource.Loaded(code())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        return Resource.Error(throwable = e, canTryAgain)
    }
}
