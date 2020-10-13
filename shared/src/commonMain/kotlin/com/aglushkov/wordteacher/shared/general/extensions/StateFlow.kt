package com.aglushkov.wordteacher.shared.general.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect

suspend fun <T> Flow<T>.forward(stateFlow: MutableStateFlow<T>) {
    collect {
        stateFlow.value = it
    }
}