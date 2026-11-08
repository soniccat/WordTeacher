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
    val isInProgress: Flow<Boolean>
    suspend fun run(srcPath: Path)
    fun isPathInProgress(srcPath: Path): Boolean
}

class DslDictImportUseCaseImpl(
    private val dictPath: Path,
    private val dslDictConverter: DslDictConverter,
    private val dslDictValidator: DslDictValidator,
    private val dictRepository: DictRepository,
): DslDictImportUseCase {
    private var runPaths = MutableStateFlow<Set<Path>>(emptySet())

    override val isInProgress: Flow<Boolean> =
        runPaths.map { it.isNotEmpty() }

    override fun isPathInProgress(srcPath: Path) =
        runPaths.value.contains(srcPath)

    override suspend fun run(srcPath: Path) {
        try {
            runPaths.update { it + srcPath }

            val pathName = srcPath.name
            val newName = dslDictConverter.convert(srcPath)

            if (dslDictValidator.validateFile(srcPath)) {
                val resolvedName = newName ?: pathName
                val importedPath = dictPath.div(resolvedName)
                srcPath.toFile().source().writeTo(importedPath.toFile().sink())
                dictRepository.importDicts()
            }

            srcPath.deleteIfExists()
        } finally {
            runPaths.update { it - srcPath }
        }
    }
}
