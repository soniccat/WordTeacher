package com.aglushkov.wordteacher.shared.repository.dict

import com.aglushkov.wordteacher.shared.dicts.dsl.DslDict
import com.aglushkov.wordteacher.shared.general.FileOpenController
import okio.FileSystem
import okio.Path

class DslDictValidator(
    val fileSystem: FileSystem
): FileOpenController.Validator {
    override fun validateFile(path: Path): Boolean {
        val dict = DslDict(path, fileSystem)
        return dict.validate()
    }
}
