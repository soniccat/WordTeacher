package com.aglushkov.wordteacher.shared.repository.dict

import co.touchlab.stately.concurrency.AtomicBoolean
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import okio.FileSystem
import okio.Path

interface DictRepository {
    val dicts: StateFlow<Resource<List<Dict>>>

    fun importDicts()
    fun wordsStartWith(prefix: String, limit: Int): List<Dict.Index.Entry>
    suspend fun define(word: String): Flow<Resource<List<WordTeacherWord>>>
}

class DictRepositoryImpl(
    private val path: Path,
    private val dictFactory: DictFactory,
    private val fileSystem: FileSystem
) : DictRepository {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    override val dicts = MutableStateFlow<Resource<List<Dict>>>(Resource.Uninitialized())
    private var trie = DictTrie() // TODO: that takes much memory, just keep a list of dicts

    var isImporting = AtomicBoolean(false)
    var needReimport = AtomicBoolean(false)

    var word: String? = null
        private set
    private var wordJob: Job? = null
    private val wordStateFlow = MutableStateFlow<Resource<List<WordTeacherWord>>>(Resource.Uninitialized())

    init {
        importDicts()
    }

    override fun importDicts() {
        scope.launch {
            importDictsInternal()
        }
    }

    private suspend fun importDictsInternal() {
        if (isImporting.compareAndSet(expected = false, new = true)) {
            if (dicts.isUninitialized()) {
                dicts.update { Resource.Loading() }
            }

            val currentDicts = dicts
            fileSystem.listOrNull(path)?.onEach { filePath ->
                val isDictLoaded = currentDicts.value.data()?.firstOrNull {
                    it.path == filePath
                } != null
                if (!isDictLoaded) {
                    dictFactory.createDict(filePath)?.let { dict ->
                        dict.load()
                        for (e in dict.index.allEntries()) {
                            trie.putWord(DictWordData(e.word, e.partOfSpeech, e.indexValue, e.dict))
                        }

                        dicts.update {
                            Resource.Loaded( (it.data() ?: emptyList()) + listOf(dict))
                        }
                    }
                }
            }

            isImporting.value = false
            if (needReimport.compareAndSet(true, false)) {
                importDictsInternal()
            }
        } else {
            needReimport.value = true
        }
    }

    override fun wordsStartWith(prefix: String, limit: Int): List<Dict.Index.Entry> {
        return trie.wordsStartWith(prefix, limit)
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
}
