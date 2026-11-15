package com.aglushkov.wordteacher.shared.usecase.dsl_dict_import_use_case

import com.aglushkov.wordteacher.shared.general.okio.deleteIfExists
import com.aglushkov.wordteacher.shared.general.okio.writeTo
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.dict.DslDictConverter
import com.aglushkov.wordteacher.shared.repository.dict.DslDictValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import okio.Path
import okio.sink
import okio.source

interface DslDictImportUseCase {
    val loadingPaths: Flow<Set<Path>>
    suspend fun run(srcPath: Path)
    fun isPathInProgress(srcPath: Path): Boolean
}

class DslDictImportUseCaseImpl(
    private val dictPath: Path,
    private val dslDictConverter: DslDictConverter,
    private val dslDictValidator: DslDictValidator,
    private val dictRepository: DictRepository,
): DslDictImportUseCase {
    override var loadingPaths = MutableStateFlow<Set<Path>>(emptySet())

    override fun isPathInProgress(srcPath: Path) =
        loadingPaths.value.contains(srcPath)

    override suspend fun run(srcPath: Path) {
        try {
            loadingPaths.update { it + srcPath }

            val pathName = srcPath.name
            val newName = dslDictConverter.convert(srcPath)

            if (dslDictValidator.validateFile(srcPath)) {
                val resolvedName = newName ?: pathName
                val importedPath = dictPath.div(resolvedName)
                dictRepository.delete(importedPath)
                srcPath.toFile().source().writeTo(importedPath.toFile().sink())
                dictRepository.importDicts()
            }

            srcPath.deleteIfExists()
        } finally {
            loadingPaths.update { it - srcPath }
        }
    }
}
