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
    private var clearables = mutableListOf<Clearable>()

    fun addClearable(clearable: Clearable) {
        clearables.add(clearable)
    }

    override fun onCleared() {
        clearables.onEach(Clearable::onCleared)
        clearables.clear()
        viewModelScope.cancel()
    }
}
