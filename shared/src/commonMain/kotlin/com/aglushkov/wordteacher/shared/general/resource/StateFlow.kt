package com.aglushkov.wordteacher.shared.general.resource

import kotlinx.coroutines.flow.StateFlow

fun <T> StateFlow<Resource<T>>?.isUninitialized(): Boolean {
    return this?.value?.isUninitialized() ?: true
}

fun <T> StateFlow<Resource<T>>?.isLoading(): Boolean {
    return this?.value?.isLoading() ?: false
}