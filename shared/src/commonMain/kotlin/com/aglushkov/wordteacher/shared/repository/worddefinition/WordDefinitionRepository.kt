package com.aglushkov.wordteacher.shared.repository.worddefinition

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.takeUntilLoadedOrErrorForVersion
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.data
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resource.isNotLoadedAndNotLoading
import com.aglushkov.wordteacher.shared.general.resource.isUninitialized
import com.aglushkov.wordteacher.shared.general.resource.toLoading
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.nlp.NLPConstants.UNKNOWN_LEMMA
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.Tag
import com.aglushkov.wordteacher.shared.model.nlp.allLemmas
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.service.WordTeacherWordService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update


class WordDefinitionRepository(
    private val serviceRepository: ServiceRepository,
    private val dictRepository: DictRepository,
    private val nlpCore: NLPCore
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlows: MutableMap<String, MutableStateFlow<Resource<List<Pair<WordTeacherWordService, List<WordTeacherWord>>>>>> = hashMapOf()
    private val jobs: MutableMap<String, Job> = hashMapOf()

    init {
        scope.launch {
            combine(
                serviceRepository.services,
                dictRepository.dicts
            ) { a, b -> Unit
                a.data().orEmpty() + b.data().orEmpty()
            }.distinctUntilChanged().collect {
                //if (it.isLoaded()) {
                    //val stateFlowsCopy = stateFlows.toMap()
                    //stateFlows.clear() // invalidate cache(), do first to make obtainMutableStateFlow returning Uninitialized

                    stateFlows.onEach {
                        it.value.update {
                            // use canLoadNextPage to mark that more data is available from new services or dicts
                            it.copy(canLoadNextPage = true)
                        }
                        //it.value.value = Resource.Uninitialized() // mark as uninitialized to notify subscribers
                    }
//                } else if (it is Resource.Error) {
//                    setNotLoadedFlowsToError(it.throwable)
//                }
            }
        }
    }

//    private fun setNotLoadedFlowsToError(throwable: Throwable) {
//        for (flowEntry in stateFlows) {
//            if (flowEntry.value.isUninitialized() || flowEntry.value.isLoading()) {
//                flowEntry.value.value = flowEntry.value.value.toError(throwable, true)
//            }
//        }
//    }

    suspend fun define(
        word: String,
        reload: Boolean = false
    ): Flow<Resource<List<Pair<WordTeacherWordService, List<WordTeacherWord>>>>> {
        val tag = "WordDefinitionRepository.define"
        val allServices = serviceRepository.services.value.data().orEmpty() +
                dictRepository.dicts.value.data().orEmpty()

        // Decide if we need to load or reuse what we've already loaded or what we're loading now
        val stateFlow = obtainMutableStateFlow(word)
        val currentStateData = stateFlow.value.data().orEmpty()
        val currentServices = currentStateData.map { it.first }
        val newServices = allServices.filter {
            !currentServices.contains(it)
        }

        val currentValue = stateFlow.value
        val needLoad = reload || currentValue.isNotLoadedAndNotLoading() || newServices.isNotEmpty()

        // Keep version for Uninitialized to support flow collecting in advance when services aren't loaded
        val nextVersion = if (needLoad && !currentValue.isUninitialized()) {
            currentValue.version + 1
        } else {
            currentValue.version
        }

        if (stateFlow.value.canLoadNextPage) {
            stateFlow.update {
                it.copy(canLoadNextPage = false)
            }
        }

        if (needLoad) {
            // Update the version of a resource as soon as possible to filter changes from the current flow if it exists
            stateFlow.update {
                it.toLoading(version = nextVersion)
            }

            jobs[word]?.cancelAndJoin()
            jobs[word] = scope.launch { // use our scope here to avoid cancellation by Structured Concurrency
                loadDefinitionsFlow(word, nextVersion, newServices).onEach { newWordsRes ->
                    //if (it.version == nextVersion) {
                        stateFlow.update {
                            it.mergeWith(newWordsRes) { _, b ->
                                currentStateData + b
                            }
//                            newWordsRes.transform(it) { newWordsData ->
//                                it.data().orEmpty() + newWordsData
//                            }
//                            it.transform(newWords) { stateFlowData ->
//                                stateFlowData + newWords.data().orEmpty()
//                            }
                        }
                    //}
                }.onCompletion { cause ->
                    cause?.let { throwable ->
                        // Keep resource state in sync after cancellation or an error
                        Logger.e("Define flow error ($nextVersion) " + throwable.message, tag)
                        //val completionValue = stateFlow.value
                        //if (completionValue.version == nextVersion) {
                            stateFlow.update { it.toError(throwable, true) }
                        //}
                    }
                }.collect()
            }
        } else {
            Logger.v("Going to reuse resource state $currentValue", tag)
        }

        return stateFlow.takeUntilLoadedOrErrorForVersion(version = nextVersion)
    }

    fun obtainStateFlow(word: String): StateFlow<Resource<List<Pair<WordTeacherWordService, List<WordTeacherWord>>>>> {
        return obtainMutableStateFlow(word)
    }

    private fun obtainMutableStateFlow(word: String): MutableStateFlow<Resource<List<Pair<WordTeacherWordService, List<WordTeacherWord>>>>> {
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
    ): Flow<Resource<List<Pair<WordTeacherWordService, List<WordTeacherWord>>>>> = channelFlow {
        // ChannelFlow to be able to emit from different coroutines
        // SupervisorScope not to interrupt when a service fails
        supervisorScope {
            val tag = "WordDefinitionRepository.loadDefinitionsFlow"
            val words = mutableListOf<Pair<WordTeacherWordService, List<WordTeacherWord>>>()
            val jobs: MutableList<Job> = mutableListOf()

            send(Resource.Loading(version = version))
            Logger.v("send Loading", tag)

            for (service in services) {
                val lemmatizer = if (service.type == Config.Type.Local) {
                    nlpCore.waitUntilLemmatizerInitialized()
                } else {
                    null
                }

                // launch instead of async/await to get results as soon as possible in a completion order
                val job = launch(CoroutineExceptionHandler { _, throwable ->
                    Logger.e("loadDefinitionsFlow Exception: " + throwable.message, service.type.toString())
                }) {
                    words.add(service to service.define(word))

                    if (lemmatizer != null) {
                        val lemmas = withContext(Dispatchers.Default) {
                            lemmatizer.allLemmas(word)
                        }

                        lemmas.onEach { lemma ->
                            words.add(service to service.define(lemma))
                        }
                    }

                    send(Resource.Loading(words.toList(), version = version))
                }

                jobs.add(job)
            }

            jobs.joinAll()

            // TODO: sort somehow if needed (consider adding this in settings)
            send(Resource.Loaded(words.toList(), version = version))
            Logger.v("send Loaded", tag)
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