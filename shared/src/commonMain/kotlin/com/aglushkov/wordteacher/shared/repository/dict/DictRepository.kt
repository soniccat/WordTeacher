package com.aglushkov.wordteacher.shared.repository.dict

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isUninitialized
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import okio.FileSystem
import okio.Path

interface DictRepository {
    val dicts: StateFlow<Resource<List<Dict>>>

    suspend fun importDicts()
    fun wordsStartWith(prefix: String, limit: Int): List<Dict.Index.Entry>
    suspend fun define(word: String): Flow<Resource<List<WordTeacherWord>>>
    fun delete(path: Path)
}

class DictRepositoryImpl(
    private val path: Path,
    private val dictFactory: DictFactory,
    private val fileSystem: FileSystem
) : DictRepository {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    override val dicts = MutableStateFlow<Resource<List<Dict>>>(Resource.Uninitialized())
    private var isImporting = MutableStateFlow(false)

    var word: String? = null
        private set
    private var wordJob: Job? = null
    private val wordStateFlow = MutableStateFlow<Resource<List<WordTeacherWord>>>(Resource.Uninitialized())

    init {
        scope.launch {
            importDicts()
        }
    }

    override suspend fun importDicts() {
        // wait until the current import finishes
        while (isImporting.value) {
            isImporting.takeWhile { it }.collect()
            if (isImporting.compareAndSet(false, true)) {
                break
            }
        }
        importDictsInternal()
        isImporting.compareAndSet(true, false)
    }

    private suspend fun importDictsInternal() {
        if (dicts.isUninitialized()) {
            dicts.update { Resource.Loading() }
        }

        val currentDicts = dicts
        val filePaths = fileSystem.listOrNull(path).orEmpty()
        if (filePaths.isEmpty()) {
            dicts.update {
                Resource.Loaded( emptyList())
            }
        } else {
            filePaths.onEach { filePath ->
                val isDictLoaded = currentDicts.value.data()?.firstOrNull {
                    it.path == filePath
                } != null
                if (!isDictLoaded) {
                    dictFactory.createDict(filePath)?.let { dict ->
                        dict.load()
                        dicts.update {
                            Resource.Loaded((it.data() ?: emptyList()) + listOf(dict))
                        }
                    }
                }
            }
        }
    }

    override fun wordsStartWith(prefix: String, limit: Int): List<Dict.Index.Entry> {
        return dicts.value.data().orEmpty()
            .map { it.index.entriesStartWith(prefix, limit) }
            .flatten()
            .sortedBy { it.word }
            .take(limit)
    }

    override suspend fun define(word: String): Flow<Resource<List<WordTeacherWord>>> {
        if (this.word == word) {
            return wordStateFlow
        }

        wordJob?.cancel()
        wordJob = scope.launch(Dispatchers.Default) {
            defineInternal(word).collect(wordStateFlow)
        }

        return wordStateFlow
    }

    private suspend fun defineInternal(word: String): Flow<Resource<List<WordTeacherWord>>> = channelFlow {
        // ChannelFlow to be able to emit from different coroutines
        // SupervisorScope not to interrupt when a service fails
        supervisorScope {
            val tag = "DictRepository.define"
            val words = mutableListOf<WordTeacherWord>()
            val jobs: MutableList<Job> = mutableListOf()

            send(Resource.Loading())

            for (dict in dicts.value.data().orEmpty()) {
                // launch instead of async/await to get results as soon as possible in a completion order
                val job = launch(CoroutineExceptionHandler { _, throwable ->
                    Logger.e("define Exception: " + throwable.message, tag)
                }) {
                    words.addAll(dict.define(word))
                    send(Resource.Loading(words.toList()))
                }

                jobs.add(job)
            }

            jobs.joinAll()

            // TODO: sort somehow if needed (consider adding this in settings)
            send(Resource.Loaded(words.toList()))
        }
    }

    override fun delete(path: Path) {
        val dict = dicts.value.data()?.firstOrNull { it.path == path } ?: return
        scope.launch(Dispatchers.Default) {
            fileSystem.delete(dict.index.path)
            fileSystem.delete(dict.path)
            dicts.update { it.mapLoadedData { it.filter { it != dict } } }
        }
    }
}
