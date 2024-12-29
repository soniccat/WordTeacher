package com.aglushkov.wordteacher.shared.dicts

import com.aglushkov.wordteacher.shared.repository.dict.DictTrie
import java.util.Locale

interface DictTrieIndex: Dict.Index {
    val index: DictTrie

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
                if (abbreviation != prefix && abbreviation != capitalizedPrefix) {
                    index.wordsStartWith(abbreviation, limit)
                } else {
                    emptyList()
                }
    }

    override fun entry(
        word: String,
        nextWordForms: () -> List<String>,
        onWordRead: () -> Unit,
        onFound: (node: MutableList<Dict.Index.Entry>) -> Unit
    ) {
        var wasFound = false
        index.entry(word, nextWordForms, onWordRead, {
            wasFound = true
            onFound(it)
        })

        if (!wasFound) {
            val lowercasedWord = word.lowercase()
            if (lowercasedWord != word) {
                index.entry(lowercasedWord, nextWordForms, onWordRead, onFound)
            }
        }
    }
}
