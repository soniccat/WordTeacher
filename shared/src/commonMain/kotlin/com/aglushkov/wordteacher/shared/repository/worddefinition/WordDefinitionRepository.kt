package com.aglushkov.wordteacher.shared.repository.worddefinition

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resource.isNotLoadedAndNotLoading
import com.aglushkov.wordteacher.shared.general.resource.isUninitialized
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.service.WordTeacherWordService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch


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

    suspend fun define(word: String): Flow<Resource<List<WordTeacherWord>>> {
        val services = serviceRepository.services.data()
        val stateFlow = obtainMutableStateFlow(word)

        return if (stateFlow.value.isLoaded()) {
            // TODO: remove after updating to new coroutine lib version
            // where distinctUntilChanged isn't built-in
            // SharedFlow with replayCapacity = 1 might be a solution
            // Here I create a copy to force StateFlow notification even if nothing has changed (the result is already Loaded)
            flowOf(stateFlow.value.toLoaded(stateFlow.value.data()!!))
        } else if (services != null && services.isNotEmpty() && stateFlow.value.isNotLoadedAndNotLoading()) {
            loadDefinitionsFlow(word, services).cancellable()
                .onEach {
                    stateFlow.value = it
                }
                .onCompletion { cause ->
                    cause?.let {
                        stateFlow.value = Resource.Error(it, true)
                    }
                }
        } else {
            Logger.v(
                "" + word + " is already loading " + stateFlow.value,
                WordDefinitionRepository::class.simpleName
            )
            emptyFlow()
        }
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

    suspend fun loadDefinitionsFlow(
        word: String
    ): Flow<Resource<List<WordTeacherWord>>> {
        val services = serviceRepository.services.data()
        return if (services == null || services.isEmpty()) {
            emptyFlow()
        } else {
            loadDefinitionsFlow(word, services)
        }
    }

    private suspend fun loadDefinitionsFlow(
        word: String,
        services: List<WordTeacherWordService>
    ): Flow<Resource<List<WordTeacherWord>>> = channelFlow {
        // channelFlow to be able to emit from different coroutines
        val words = mutableListOf<WordTeacherWord>()
        val jobs: MutableList<Job> = mutableListOf()

        send(Resource.Loading())
        for (service in services) {
            // launch instead of async/await to get results as soon as possible in a completion order
            val job = launch {
                words.addAll(service.define(word))
                send(Resource.Loading(words.toList()))
            }

            jobs.add(job)
        }

        jobs.joinAll()

        // TODO: sort somehow if needed (consider adding this in settings)
        send(Resource.Loaded(words.toList()))
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