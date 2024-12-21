package com.aglushkov.wordteacher.shared.dicts.wordlist

import com.aglushkov.wordteacher.shared.dicts.Dict
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
): Dict.Index {
    private val index = DictTrie()
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

    override fun allEntries(): Sequence<Dict.Index.Entry> {
        return index.asSequence()
    }

    override fun indexEntry(word: String): Dict.Index.Entry? {
        return index.word(word).firstOrNull()
    }

    override fun entriesStartWith(prefix: String, limit: Int): List<Dict.Index.Entry> {
        if (prefix.isEmpty()) {
            return emptyList()
        }

        // search for capitalized, lowercased and actual prefix
        val capitalizedPrefix = prefix.replaceFirstChar {
            if (it.isLowerCase()) {
                it.uppercaseChar()
            } else {
                it.lowercaseChar()
            }
        }
        val abbreviation = if (prefix[0].isLowerCase()) {
            prefix.uppercase(Locale.getDefault())
        } else {
            prefix.lowercase(Locale.getDefault())
        }

        return index.wordsStartWith(prefix, limit) +
                index.wordsStartWith(capitalizedPrefix, limit) +
                index.wordsStartWith(abbreviation, limit)
    }

    override fun entry(
        word: String,
        nextWordForms: () -> List<String>,
        onWordRead: () -> Unit,
        onFound: (node: MutableList<Dict.Index.Entry>) -> Unit
    ) {
        return index.entry(word, nextWordForms, onWordRead, onFound)
    }
}


const val WORDLIST_EXTENSION = "wordlist"