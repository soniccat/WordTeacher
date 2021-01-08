package com.aglushkov.wordteacher.shared.repository.service

import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isNotLoadedAndNotLoading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfigConnectParamsStatRepository(
    private val file: ConfigConnectParamsStatFile
) {
    private val stateFlow = MutableStateFlow<Resource<List<ConfigConnectParamsStat>>>(Resource.Uninitialized())
    val flow: StateFlow<Resource<List<ConfigConnectParamsStat>>> = stateFlow
    val value: Resource<List<ConfigConnectParamsStat>>
        get() {
            return stateFlow.value
        }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        loadIfNeeded()
    }

    fun loadIfNeeded() {
        if (value.isNotLoadedAndNotLoading()) {
            load()
        }
    }

    private fun load() {
        scope.launch {
            loadConfigConnectParamsStatFlow().onStart {
                stateFlow.value.toLoading()
            }.forward(stateFlow)
        }
    }

    fun addStat(stat: ConfigConnectParamsStat) {
        val list: MutableList<ConfigConnectParamsStat> = stateFlow.value.data()?.toMutableList() ?: mutableListOf()
        list.add(stat)
        stateFlow.value = Resource.Loaded(list)
        saveConfigConnectParamsStat()
    }

    private fun loadConfigConnectParamsStatFlow() = flow {
        try {
            val configs = withContext(Dispatchers.Default) {
                val bytes = file.loadContent()
                ConfigConnectParamsStat.fromByteArray(bytes)
            }
            emit(Resource.Loaded(configs))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            emit(Resource.Loaded<List<ConfigConnectParamsStat>>(emptyList()))
        }
    }

    private fun saveConfigConnectParamsStat() = scope.launch {
        val value = stateFlow.value.data() ?: return@launch

        try {
            withContext(Dispatchers.Default) {
                val bytes = value.toByteArray()
                file.saveContent(bytes)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}