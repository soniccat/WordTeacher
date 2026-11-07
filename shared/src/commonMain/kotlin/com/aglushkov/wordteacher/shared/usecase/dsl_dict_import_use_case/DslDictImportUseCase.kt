package com.aglushkov.wordteacher.shared.usecase.dsl_dict_import_use_case

import com.aglushkov.wordteacher.shared.general.okio.deleteIfExists
import com.aglushkov.wordteacher.shared.general.okio.writeTo
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.dict.DslDictConverter
import com.aglushkov.wordteacher.shared.repository.dict.DslDictValidator
import okio.Path
import okio.sink
import okio.source

interface DslDictImportUseCase {
    suspend fun run(srcPath: Path)
}

class DslDictImportUseCaseImpl(
    private val dictPath: Path,
    private val dslDictConverter: DslDictConverter,
    private val dslDictValidator: DslDictValidator,
    private val dictRepository: DictRepository,
): DslDictImportUseCase {
    override suspend fun run(srcPath: Path) {
        val pathName = srcPath.name
        val newName = dslDictConverter.convert(srcPath)
        val resolvedName = newName ?: pathName
//        val covertedPath = srcPath.parent!!.div(resolvedName)
        val importedPath = dictPath.div(resolvedName)

        if (dslDictValidator.validateFile(srcPath)) {
            srcPath.toFile().source().writeTo(importedPath.toFile().sink())
            dictRepository.importDicts()
        }

        srcPath.deleteIfExists()
    }
}
