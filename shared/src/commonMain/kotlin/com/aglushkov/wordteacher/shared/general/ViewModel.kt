package com.aglushkov.wordteacher.shared.general

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

interface Clearable {
    fun onCleared()
}

open class ViewModel: Clearable {
    val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCleared() {
        viewModelScope.cancel()
    }
}