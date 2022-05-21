package com.aglushkov.wordteacher.shared.repository.dict_word_filter_list

import co.touchlab.stately.concurrency.AtomicBoolean
import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.collectUntilLoaded
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isUninitialized
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import okio.FileSystem
import okio.Path

interface DictWordFilterListRepository {
    // dict name to a list of filtered words
    val dictFilters: StateFlow<Resource<Map<String, List<String>>>>

    fun addFilter(dictName: String, word: String)
    fun removeFilter(dictName: String, word: String)
}

// TODO: finish
//class DictWordFilterListRepositoryImpl(
//    private val path: Path,
//    private val fileSystem: FileSystem,
//    private val dictRepository: DictRepository
//) : DictWordFilterListRepository {
//    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
//
////    private val files: StateFlow<List<DictFilter>> = dictRepository.dicts.map {
////
////    }.stateIn(scope, SharingStarted.Eagerly, Resource.Uninitialized())
////        //MutableStateFlow<Resource<List<DictFilter>>>(Resource.Uninitialized())
//
//    var word: String? = null
//        private set
//    private var wordJob: Job? = null
//    private val wordStateFlow = MutableStateFlow<Resource<List<WordTeacherWord>>>(Resource.Uninitialized())
//
//    init {
//    }
//
//
//}
