package com.aglushkov.wordteacher.shared.repository.worddefinition

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resource.isNotLoadedAndNotLoading
import com.aglushkov.wordteacher.shared.general.resource.isUninitialized
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.service.WordTeacherWordService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope


class WordDefinitionRepository(
    private val serviceRepository: ServiceRepository,
) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlows: MutableMap<String, MutableStateFlow<Resource<List<WordTeacherWord>>>> = hashMapOf()

    init {
        scope.launch {
            serviceRepository.flow.collect {
                if (it.isLoaded()) {
                    defineUninitializedFlows()
                    // TODO: consider to handle adding new services
                } else if (it is Resource.Error) {
                    setNotLoadedFlowsToError(it.throwable)
                }
            }
        }
    }

    private suspend fun defineUninitializedFlows() {
        for (flowEntry in stateFlows) {
            if (flowEntry.value.isUninitialized()) {
                define(flowEntry.key)
            }
        }
    }

    private fun setNotLoadedFlowsToError(throwable: Throwable) {
        for (flowEntry in stateFlows) {
            if (flowEntry.value.isUninitialized() || flowEntry.value.isLoading()) {
                flowEntry.value.value = flowEntry.value.value.toError(throwable, true)
            }
        }
    }

    suspend fun define(word: String): StateFlow<Resource<List<WordTeacherWord>>> {
        val services = serviceRepository.services.data()
        val stateFlow = obtainMutableStateFlow(word)

        if (services != null && services.isNotEmpty() && stateFlow.value.isNotLoadedAndNotLoading()) {
            loadDefinitions(word, services, stateFlow)
        }

        return stateFlow
    }

    fun obtainStateFlow(word: String): StateFlow<Resource<List<WordTeacherWord>>> {
        return obtainMutableStateFlow(word)
    }

    private fun obtainMutableStateFlow(word: String): MutableStateFlow<Resource<List<WordTeacherWord>>> {
        var stateFlow = stateFlows[word]
        if (stateFlow == null) {
            stateFlow = MutableStateFlow(Resource.Uninitialized())
            stateFlows[word] = stateFlow
        }

        return stateFlow
    }

    private suspend fun loadDefinitions(
        word: String,
        services: List<WordTeacherWordService>,
        stateFlow: MutableStateFlow<Resource<List<WordTeacherWord>>>
    ) = supervisorScope {
        stateFlow.value = stateFlow.value.toLoading()

        try {
            val words = mutableListOf<WordTeacherWord>()
            val jobs: MutableList<Job> = mutableListOf()

            stateFlow.value = stateFlow.value.toLoading(words.toList())
            for (service in services) {
                // launch instead of async/await to get results as soon as possible in a completion order
                val job = launch(CoroutineExceptionHandler { _, throwable ->
                    Logger.e("Define Exception: " + throwable.message, service.type.toString())
                }) {
                    words.addAll(service.define(word))
                    stateFlow.value = stateFlow.value.toLoading(words.toList())
                }

                jobs.add(job)
            }

            jobs.joinAll()

            // TODO: sort somehow if needed (consider adding this in settings)
            stateFlow.value = stateFlow.value.toLoaded(words.toList())
        } catch (e: Exception) {
            stateFlow.value = stateFlow.value.toError(e, true)
        }
    }

    fun clear(word: String) {
        for (flowEntry in stateFlows) {
            if (flowEntry.key == word) {
                flowEntry.value.value = Resource.Uninitialized()
                stateFlows.remove(flowEntry.key)
                return
            }
        }
    }
}