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
    private val fileSystem: FileSystem
): Dict {
    override var type = Config.Type.Local
    override var name = ""
    override var fromLang = Language.EN
    override var toLang = Language.EN

    override lateinit var index: WordListDictIndex

    override suspend fun load() {
        this.index = WordListDictIndex(this, (path.toString()).toPath(), fileSystem)
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
): DictTrieIndex {
    override val index = DictTrie()
    private val wordListWordData = DictWordData(WordTeacherWord.PartOfSpeech.Undefined, null, dict)

    init {
        if (fileSystem.exists(path)) {
            try {
                loadIndex()
            } catch (e: Throwable) {
                fileSystem.delete(path)
            }
        }
    }

    private fun loadIndex() {
        fileSystem.read(path) {
            while(!exhausted()) {
                readUtf8Line()?.let { word ->
                    index.put(word, wordListWordData)
                }
            }
        }
    }
}

const val WORDLIST_EXTENSION = "wordlist"