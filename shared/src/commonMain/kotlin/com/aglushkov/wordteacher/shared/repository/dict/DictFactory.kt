package com.aglushkov.wordteacher.shared.repository.dict

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.dsl.DslDict
import okio.FileSystem
import okio.Path

class DictFactory(
    private val fileSystem: FileSystem
) {
    fun createDict(path: Path): Dict? {
        return when {
            path.name.endsWith("dsl") -> DslDict(path, fileSystem)
            else -> null
        }
    }
}
