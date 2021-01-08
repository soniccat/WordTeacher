package com.aglushkov.wordteacher.shared.repository.worddefinition

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.takeUntilLoadedOrErrorForVersion
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isError
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resource.isNotLoadedAndNotLoading
import com.aglushkov.wordteacher.shared.general.resource.isUninitialized
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.service.WordTeacherWordService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope


class WordDefinitionRepository(
    private val serviceRepository: ServiceRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlows: MutableMap<String, MutableStateFlow<Resource<List<WordTeacherWord>>>> = hashMapOf()
    private val jobs: MutableMap<String, Job> = hashMapOf()

    val servicesFlow = serviceRepository.flow

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
                define(flowEntry.key, false)
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

    suspend fun define(
        word: String,
        reload: Boolean = false
    ): Flow<Resource<List<WordTeacherWord>>> {
        // ignore if we have no services
        val services = serviceRepository.services.data()
        if (services == null || services.isEmpty()) {
            Logger.v("No services")
            return emptyFlow()
        }

        // decide if we need to load or reuse what we've already loaded or what we're loading now
        val stateFlow = obtainMutableStateFlow(word)
        val currentValue = stateFlow.value
        val needLoad = reload || currentValue.isError() || currentValue.isNotLoadedAndNotLoading()

        val nextVersion = if (needLoad) {
            currentValue.version + 1
        } else {
            currentValue.version
        }

        // launch loading flow
        if (needLoad) {
            // update the version of a resource as soon as possible to filter changes from the current flow if it exists
            stateFlow.value = Resource.Loading(version = nextVersion)

            jobs[word]?.cancel()
            jobs[word] = scope.launch { // use our scope here to avoid cancellation by Structured Concurrency
//                val currentValue = stateFlow.value
//                val loadFlow = if (needLoad) {
//                    Logger.v("called loadDefinitionsFlow")
//                    loadDefinitionsFlow(word, nextVersion, services)
//                } else if (currentValue is Resource.Loaded) {
//                    Logger.v("called flowOf with Loaded")
//                    flowOf(currentValue.toLoaded(currentValue.data, version = nextVersion))
//                } else {
//                    Logger.v(
//                        "" + word + " is already loading " + stateFlow.value,
//                        WordDefinitionRepository::class.simpleName
//                    )
//                    emptyFlow()
//                }

                loadDefinitionsFlow(word, nextVersion, services).onEach {
                    if (it.version == nextVersion) {
                        stateFlow.value = it
                    }
                }.onCompletion { cause ->
                    cause?.let {
                        // keep resource state in sync after cancellation or an error
                        Logger.e("Define flow error (${nextVersion}) " + it.message)
                        val completionValue = stateFlow.value
                        if (completionValue.version == nextVersion) {
                            stateFlow.value = Resource.Error(it, true)
                        }
                    }
                }.collect()
            }
        }

        return stateFlow.takeUntilLoadedOrErrorForVersion()
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

    private suspend fun loadDefinitionsFlow(
        word: String,
        version: Int,
        services: List<WordTeacherWordService>
    ): Flow<Resource<List<WordTeacherWord>>> = channelFlow {
        // channelFlow to be able to emit from different coroutines
        // supervisorScope not to interrupt when a service fails
        supervisorScope {
            val words = mutableListOf<WordTeacherWord>()
            val jobs: MutableList<Job> = mutableListOf()

            send(Resource.Loading(version = version))
            Logger.v("loadDefinitionsFlow set Loading")

            for (service in services) {
                // launch instead of async/await to get results as soon as possible in a completion order
                val job = launch(CoroutineExceptionHandler { _, throwable ->
                    Logger.e("loadDefinitionsFlow Exception: " + throwable.message, service.type.toString())
                }) {
                    words.addAll(service.define(word))
                    send(Resource.Loading(words.toList(), version = version))
                }

                jobs.add(job)
            }

            jobs.joinAll()

            // TODO: sort somehow if needed (consider adding this in settings)
            send(Resource.Loaded(words.toList(), version = version))
            Logger.v("loadDefinitionsFlow set Loaded")
        }
    }

    fun clear(word: String) {
        for (flowEntry in stateFlows) {
            if (flowEntry.key == word) {
                val res = flowEntry.value
                res.value = Resource.Uninitialized(version = res.value.version + 1)
                stateFlows.remove(flowEntry.key)
                return
            }
            jobs[word]?.cancel()
        }
    }
}