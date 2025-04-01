package com.aglushkov.wordteacher.shared.general.resource

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.js.JsName
import kotlin.math.min


sealed interface Resource<T> {
    val canLoadNextPage: Boolean // TODO: rename to canLoadMore to be able to use for stale data
    val version: Int

    data class Uninitialized<T>(override val canLoadNextPage: Boolean = false, override val version: Int = 0) : Resource<T>
    data class Loaded<T>(val data: T, override val canLoadNextPage: Boolean = false, override val version: Int = 0) : Resource<T>
    data class Loading<T>(val data: T? = null, val progress: Float = 0.0f, override val canLoadNextPage: Boolean = false, override val version: Int = 0) : Resource<T>
    data class Error<T>(val throwable: Throwable, val canTryAgain: Boolean = true, val data: T? = null, override val canLoadNextPage: Boolean = false, override val version: Int = 0) : Resource<T>

    fun toUninitialized(canLoadNextPage: Boolean = this.canLoadNextPage, version: Int = this.version) = Uninitialized<T>(canLoadNextPage, version)
    fun toLoading(data: T? = data(), progress: Float = 0.0f, canLoadNext: Boolean = this.canLoadNextPage, version: Int = this.version) = Loading(data, progress, canLoadNext, version)
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

    fun progress(): Float {
        return when (this) {
            is Loaded -> 1.0f
            is Loading -> progress
            else -> 0.0f
        }
    }

    fun throwable(): Throwable? {
        return when (this) {
            is Error -> this.throwable
            else -> null
        }
    }

    fun canTryAgain(): Boolean {
        return when (this) {
            is Error -> this.canTryAgain
            else -> false
        }
    }

    // change resource data with a value of another type
    fun <R> copyWith(data: R?, canLoadNextPage: Boolean = this.canLoadNextPage, errorTransformer: ((Throwable) -> Throwable)? = null, version: Int = this.version): Resource<R> {
        return map(
            canLoadNextPage = canLoadNextPage,
            errorTransformer = errorTransformer,
            version = version,
        ) { data }
    }

    // change resource data with default data value if it's null (in loading for example)
    // if defaultData is null then transform won't be called for null data
    fun <R> mapLoadedData(
        canLoadNextPage: Boolean = this.canLoadNextPage,
        errorTransformer: ((Throwable) -> Throwable)? = null,
        version: Int = this.version,
        defaultData: T? = null,
        loadedDataTransformer: (T) -> R?,
    ): Resource<R> = map(
        canLoadNextPage = canLoadNextPage,
        errorTransformer = errorTransformer,
        version = version,
    ) {
        (it ?: defaultData)?.let { resultData ->
            loadedDataTransformer(resultData)
        }
    }

    // change resource data with a block
    fun <R> map(
        canLoadNextPage: Boolean = this.canLoadNextPage,
        errorTransformer: ((Throwable) -> Throwable)? = null,
        version: Int = this.version,
        loadedDataTransformer: (T?) -> R?
    ): Resource<R> = when (this) {
        is Loaded -> Loaded(
            data = loadedDataTransformer(data) ?: throw  RuntimeException("map: nil data for Loaded resource isn't supported"),
            canLoadNextPage = canLoadNextPage,
            version = version
        )
        is Loading -> Loading(
            data = loadedDataTransformer(data),
            progress = progress,
            canLoadNextPage = canLoadNextPage,
            version = version
        )
        is Error -> Error(
            throwable = errorTransformer?.invoke(throwable) ?: throwable,
            canTryAgain = canTryAgain,
            data = loadedDataTransformer(data),
            canLoadNextPage = canLoadNextPage,
            version = version
        )
        is Uninitialized -> Uninitialized(
            canLoadNextPage = canLoadNextPage,
            version = version
        )
    }

    // take state from another resource
//    fun <R> mergeWith(
//        res: Resource<R>,
//        canLoadNextPageTransformer: (Boolean, Boolean) -> Boolean = { a, _ -> a },
//        versionTransformer: (Int, Int) -> Int = { a, _ -> a },
//        throwableTransformer: (Throwable?, Throwable) -> Throwable = { a, b -> a ?: b },
//        canTryAgainTransformer: (Boolean, Boolean) -> Boolean = { a, _ -> a },
//        dataTransformer: (T?, R) -> T?
//    ): Resource<T> = when (res) {
//        is Loaded -> this.toLoaded(
//            data = dataTransformer(data(), res.data) ?: throw RuntimeException("mergeWith: nil data for Loaded resource isn't supported"),
//            canLoadNext = canLoadNextPageTransformer(canLoadNextPage, res.canLoadNextPage),
//            version = versionTransformer(version, res.version)
//        )
//        is Loading -> this.toLoading(
//            data = res.data?.let { dataTransformer(data(), it) } ?: data(),
//            canLoadNext = canLoadNextPageTransformer(canLoadNextPage, res.canLoadNextPage),
//            version = versionTransformer(version, res.version)
//        )
//        is Error -> this.toError(
//            throwable = throwable()?.let { throwableTransformer(it, res.throwable) } ?: res.throwable,
//            data = res.data?.let { dataTransformer(data(), it) } ?: data(),
//            canTryAgain = canTryAgainTransformer(canTryAgain(), res.canTryAgain),
//            canLoadNext = canLoadNextPageTransformer(canLoadNextPage, res.canLoadNextPage),
//            version = versionTransformer(version, res.version)
//        )
//        is Uninitialized -> this.toUninitialized(
//            canLoadNextPage = canLoadNextPageTransformer(canLoadNextPage, res.canLoadNextPage),
//            version = versionTransformer(version, res.version)
//        )
//    }

    // TODO: remove and replace with map
    fun updateData(
        block: (T?) -> T?
    ): Resource<T> = when (this) {
        is Loaded -> this.toLoaded(data = block(data) ?: throw RuntimeException("updateData: nil data for Loaded resource isn't supported"))
        is Loading -> this.toLoading(data = block(data), progress = progress)
        is Error -> this.toError(throwable = throwable, canTryAgain = canTryAgain, data = block(data))
        is Uninitialized -> this.toUninitialized()
    }

    fun updateCanLoadNextPage(
        canLoadNextPage: Boolean,
    ): Resource<T> = when (this) {
        is Loaded -> this.toLoaded(data, canLoadNext = canLoadNextPage)
        is Loading -> this.toLoading(canLoadNext = canLoadNextPage, progress = progress)
        is Error -> this.toError(throwable = throwable, canTryAgain = canTryAgain, canLoadNext = canLoadNextPage)
        is Uninitialized -> this.toUninitialized(canLoadNextPage = canLoadNextPage)
    }

    fun bumpVersion(): Resource<T> = when (this) {
        is Loaded -> this.toLoaded(data, version = version + 1)
        is Loading -> this.toLoading(version = version + 1, progress = progress)
        is Error -> this.toError(throwable = throwable, canTryAgain = canTryAgain, version = version + 1)
        is Uninitialized -> this.toUninitialized(version = version + 1)
    }
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
    return this?.toLoading(progress = 0.0f) ?: Resource.Loading(progress = 0.0f)
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
    return if (this == null) false else (this is Resource.Loaded || this is Resource.Loading)
}

fun <T> Resource<T>.on(
    uninitialized: (() -> Unit)? = null,
    data: ((T) -> Unit)? = null,
    loadingOrError: ((Throwable?) -> Unit)? = null,
    loaded: ((T) -> Unit)? = null,
    loading: ((T?) -> Unit)? = null,
    error: ((Throwable) -> Unit)? = null
) {
    uninitialized?.let { onUnitialized(it) }
    val onDataCalled = data?.let { onData(data) } ?: false
    if (!onDataCalled) {
        loaded?.let { onLoaded(block = it) }

        val onLoadingOrErrorCalled = loadingOrError?.let {
            if (isLoading() || isError()) {
                loadingOrError(this.asError()?.throwable)
                true
            } else {
                false
            }
        } ?: false

        if (!onLoadingOrErrorCalled) {
            loading?.let { onLoading(it) }
            error?.let { onError(it) }
        }
    }
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

fun <T> Resource<T>.onLoaded(elseBlock: () -> Unit = {}, block: (T) -> Unit): Boolean {
    return when (this) {
        is Resource.Loaded -> {
            block(data)
            true
        }
        else -> {
            elseBlock()
            false
        }
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
    } ?: false
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
        return Resource.Loading(transform(data(), res2.data()), progress = min(progress(), res2.progress()))
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
        emit(initialValue.toLoaded(result))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.e(e.message.orEmpty(), "loadResource")
        e.printStackTrace()
        emit(initialValue.toError(e, canTryAgain))
    }
}

fun <T> loadResourceWithProgress(
    initialValue: Resource<T> = Resource.Uninitialized(),
    canTryAgain: Boolean = true,
    loader: Flow<Pair<Float, T?>>
): Flow<Resource<T>> = flow {
    try {
        emit(initialValue.toLoading())
        var lastValue: T? = null
        loader.collect { (progress, result) ->
            lastValue = result
            emit(initialValue.toLoading(result, progress))
        }
        emit(Resource.Loaded(lastValue ?: throw RuntimeException("loadResourceWithProgress: loaded data is null")))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.e(e.message.orEmpty(), "loadResource")
        e.printStackTrace()
        emit(initialValue.toError(e, canTryAgain))
    }
}

const val RESOURCE_UNDEFINED_PROGRESS = -1.0f