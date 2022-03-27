package com.aglushkov.wordteacher.shared

import com.aglushkov.wordteacher.shared.dicts.dsl.DslDict
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

fun createFakeDict(content: String): DslDict {
    val fakeFileSystem = FakeFileSystem()
    val dirPath = "/test".toPath()
    val dictPath = dirPath.div("dict.dsl")
    fakeFileSystem.createDirectories(dirPath)
    fakeFileSystem.write(dictPath, true) {
        writeUtf8(content)
    }

    val dict = DslDict(dictPath, fakeFileSystem)
    return dict
}