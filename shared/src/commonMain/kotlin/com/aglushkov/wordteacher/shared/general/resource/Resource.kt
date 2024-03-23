package com.aglushkov.wordteacher.shared.general.resource

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.js.JsName


sealed interface Resource<T> {
    val canLoadNextPage: Boolean
    val version: Int

    data class Uninitialized<T>(override val canLoadNextPage: Boolean = false, override val version: Int = 0) : Resource<T>
    data class Loaded<T>(val data: T, override val canLoadNextPage: Boolean = false, override val version: Int = 0) : Resource<T>
    data class Loading<T>(val data: T? = null, override val canLoadNextPage: Boolean = false, override val version: Int = 0) : Resource<T>
    data class Error<T>(val throwable: Throwable, val canTryAgain: Boolean = true, val data: T? = null, override val canLoadNextPage: Boolean = false, override val version: Int = 0) : Resource<T>

    fun toUninitialized(canLoadNextPage: Boolean = this.canLoadNextPage, version: Int = this.version) = Uninitialized<T>(canLoadNextPage, version)
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
            is Uninitialized -> Uninitialized(version = version)
            is Loaded -> Loaded(data!!, canLoadNextPage, version)
            is Loading -> Loading(data, canLoadNextPage, version)
            is Error -> Error(throwable, canTryAgain, data, canLoadNextPage, version)
        }
    }

    // it's better to use transform instead not to access this.data outside
    fun <R> copyWith(data: R?): Resource<R> {
        return when(this) {
            is Uninitialized -> Uninitialized(version = version)
            is Loaded -> Loaded(data!!, canLoadNextPage, version) // TODO: force unwrap looks dangerous
            is Loading -> Loading(data, canLoadNextPage, version)
            is Error -> Error(throwable, canTryAgain, data, canLoadNextPage, version)
        }
    }

    fun <R> transform(
        from: Resource<R> = Uninitialized(),
        errorTransformer: ((Throwable) -> Throwable)? = null,
        loadedDataTransformer: (T) -> R
    ): Resource<R> = when (this) {
        is Loaded -> from.toLoaded(data = loadedDataTransformer(data))
        is Loading -> from.toLoading(data = data?.let { loadedDataTransformer(it) }, canLoadNextPage, version)
        is Error -> from.toError(throwable, canTryAgain, data = data?.let { loadedDataTransformer(it) }, canLoadNextPage, version)
        is Uninitialized -> from.toUninitialized()
    }

    fun bumpVersion() = copy(version = this.version + 1)
}


fun <T,R> Resource<T>.downgradeToErrorOrLoading(r: Resource<R>): Resource<T> = when(this) {
    is Resource.Loaded -> when(r) {
        is Resource.Error -> this.toError(r.throwable, r.canTryAgain)
        is Resource.Loading -> this.toLoading()
        is Resource.Loaded, is Resource.Uninitialized -> this
    }
    is Resource.Loading -> when(r) {
        is Resource.Error -> this.toError(r.throwable, r.canTryAgain)
        is Resource.Loading, is Resource.Loaded, is Resource.Uninitialized -> this
    }
    is Resource.Error, is Resource.Uninitialized -> this
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

fun <T> Resource<T>.asLoaded(): Resource.Loaded<T>? {
    return when(this) {
        is Resource.Loaded -> this
        else -> null
    }
}

fun <T> Resource<T>.asError(): Resource.Error<T>? {
    return when(this) {
        is Resource.Error -> this
        else -> null
    }
}

fun <T> Resource<T>.asLoadedOrError(): Resource<T>? {
    return when(this) {
        is Resource.Loaded -> this
        is Resource.Error -> this
        else -> null
    }
}

fun <T> Resource<T>?.toLoading(): Resource.Loading<T> {
    return this?.toLoading() ?: Resource.Loading()
}

fun Resource<*>?.isNotLoadedAndNotLoading(): Boolean {
    return if (this == null) false else this !is Resource.Loaded && this !is Resource.Loading
}

fun Resource<*>?.isNotLoadedAndNotError(): Boolean {
    return if (this == null) false else this !is Resource.Loaded && this !is Resource.Error
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

fun Resource<*>?.isLoadedOrLoading(): Boolean {
    return if (this == null) false else (this !is Resource.Loaded || this !is Resource.Error)
}

fun <T> Resource<T>.on(
    uninitialized: (() -> Unit)? = null,
    loaded: ((T) -> Unit)? = null,
    loading: ((T?) -> Unit)? = null,
    error: ((Throwable) -> Unit)? = null
) {
    uninitialized?.let { onUnitialized(it) }
    loaded?.let { onLoaded(it) }
    loading?.let { onLoading(it) }
    error?.let { onError(it) }
}

fun <T> Resource<T>.onUnitialized(block: () -> Unit): Boolean {
    return when (this) {
        is Resource.Uninitialized -> {
            block()
            true
        }
        else -> false
    }
}

fun <T> Resource<T>.onLoaded(block: (T) -> Unit): Boolean {
    return when (this) {
        is Resource.Loaded -> {
            block(data)
            true
        }
        else -> false
    }
}

fun <T> Resource<T>.onLoading(block: (T?) -> Unit): Boolean {
    return when (this) {
        is Resource.Loading -> {
            block(data)
            true
        }
        else -> false
    }
}

fun <T> Resource<T>.onData(block: (T) -> Unit): Boolean {
    return data()?.let {
        block(it)
        true
    } ?: run {
        false
    }
}

fun Resource<*>.onError(block: (Throwable) -> Unit) {
    when (this) {
        is Resource.Error -> block(throwable)
        else -> return
    }
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
    return try {
        Resource.Loaded(code())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Logger.e(e.message.toString(), "tryInResource")
        Resource.Error(throwable = e, canTryAgain)
    }
}

fun <T> loadResource(
    initialValue: Resource<T> = Resource.Uninitialized(),
    canTryAgain: Boolean = true,
    loader: suspend () -> T
): Flow<Resource<T>> = flow {
    try {
        emit(initialValue.toLoading())
        val result = loader()
        emit(Resource.Loaded(result))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.e(e.message.orEmpty(), "loadResource")
        e.printStackTrace()
        emit(initialValue.toError(e, canTryAgain))
    }
}
