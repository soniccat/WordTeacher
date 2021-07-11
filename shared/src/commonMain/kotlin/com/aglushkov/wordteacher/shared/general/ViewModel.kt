package com.aglushkov.wordteacher.shared.general

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

open class ViewModel {
    val viewModelScope = CoroutineScope(Dispatchers.Main)

    open fun onCleared() {
        viewModelScope.cancel()
    }
}