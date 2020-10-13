package com.aglushkov.wordteacher.shared.repository

import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isNotLoadedAndNotLoading
import com.aglushkov.wordteacher.shared.service.ConfigService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class ConfigRepository(
    val service: ConfigService,
    private val connectivityManager: ConnectivityManager
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val stateFlow = MutableStateFlow<Resource<List<Config>>>(Resource.Uninitialized())
    val flow = stateFlow
    val value: Resource<List<Config>>
        get() {
            return stateFlow.value
        }

    init {
        loadIfNeeded()

        // load a config on connecting to the internet
        scope.launch {
            connectivityManager.flow.collect {
                if (it) {
                    loadIfNeeded()
                }
            }
        }
    }

    fun loadIfNeeded() {
        if (value.isNotLoadedAndNotLoading()) {
            load()
        }
    }

    private fun load() {
        scope.launch {
            loadConfigFlow().onStart {
                stateFlow.value.toLoading()
            }.forward(stateFlow)
        }
    }

    private fun loadConfigFlow() = flow {
        try {
            val configs = service.config()
            emit(Resource.Loaded(configs))
        } catch (e: Exception) {
            emit(stateFlow.value.toError(e, true))
        }
    }
}
