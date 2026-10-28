package com.aglushkov.wordteacher.shared.dicts.wordlist

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.DictTrieIndex
import com.aglushkov.wordteacher.shared.dicts.Language
import com.aglushkov.wordteacher.shared.general.okio.writeTo
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.dict.DictTrie
import com.aglushkov.wordteacher.shared.repository.dict.DictWordData
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Source
import java.util.Locale

// WordListDict works just like an index holder to provide suggestions and word highlighting
// without definitions
class WordListDict(
    override val path: Path,
    private val fileSystem: FileSystem,
    private val lastVersion: Int,
): Dict {
    override var type = Config.Type.Local
    override var name = ""
    override var fromLang = Language.EN
    override var toLang = Language.EN

    override lateinit var index: WordListDictIndex

    override suspend fun load() {
        this.index = WordListDictIndex(this, (path.toString()).toPath(), fileSystem, lastVersion)
    }

    override suspend fun define(word: String, indexEntry: Dict.Index.Entry): List<WordTeacherWord> {
        // the class not intended to get find definitions
        return emptyList()
    }

    override suspend fun define(words: List<String>): List<WordTeacherWord> {
        // the class not intended to get find definitions
        return emptyList()
    }
}

class WordListDictIndex(
    private val dict: Dict,
    override val path: Path,
    private val fileSystem: FileSystem,
    lastVersion: Int,
    var version: Int = -1,
): DictTrieIndex {
    override val index = DictTrie()
    private val wordListWordData = DictWordData(emptyList(), null, dict)
    var wordCount = 0

    init {
        if (fileSystem.exists(path)) {
            try {
                loadIndex(lastVersion)
            } catch (e: Throwable) {
                fileSystem.delete(path)
                throw e
            }
        }
    }

    private fun loadIndex(lastVersion: Int) {
        fileSystem.read(path) {
            val line = readUtf8Line()
            if (line?.startsWith(WORDLIST_VERSION_PREFIX) == true) {
                val v = line.substring(WORDLIST_VERSION_PREFIX.length).toInt()
                version = v
            }

            if (line == null || version != lastVersion) {
                // skip loading outdated version, expect new version after importDicts in CopyDictTask
                throw RuntimeException("WordListDictIndex: outdated version $version, expect $lastVersion")
            }

            while(!exhausted()) {
                readUtf8Line()?.let { word ->
                    index.put(word, wordListWordData)
                    ++wordCount
                }
            }
        }
    }
}

const val WORDLIST_EXTENSION = "wordlist"
const val WORDLIST_VERSION_PREFIX = "version:"