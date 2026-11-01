package com.aglushkov.wordteacher.shared

import com.aglushkov.wordteacher.shared.dicts.dsl.DslDict
import kotlinx.coroutines.runBlocking
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

    runBlocking {
        dict.load()
    }

    return dict
}

fun buildDictContent(actionsCallback: DslDictActions.() -> Unit): String {
    val actions = DslDictActions()
    actionsCallback.invoke(actions)
    return actions.build()
}

class DslDictActions {
    var sb = StringBuilder()

    fun addTerm(term: String, defs: List<String>) {
        sb.append(term + '\n')

        defs.onEach(::addDef)
    }

    fun addDef(def: String) {
        sb.append("\t[m1]1) [trn]$def[/trn][/m]\n")
    }

    fun build() = sb.toString()
}