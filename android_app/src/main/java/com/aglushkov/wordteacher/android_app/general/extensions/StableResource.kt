package com.aglushkov.wordteacher.android_app.general.extensions

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoadedAndEmpty
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.isLoading

@Stable
@Immutable
data class StableResource<T>(
    val resource: Resource<T>
) {
    fun data(): T? {
        return resource.data()
    }
}

fun <T> Resource<T>.toStableResource() = StableResource(this)

fun StableResource<*>.isLoading(): Boolean = resource.isLoading()

fun <T> StableResource<T>.isLoadedAndEmpty(): Boolean where T : Collection<*> {
    return resource.isLoadedAndEmpty()
}