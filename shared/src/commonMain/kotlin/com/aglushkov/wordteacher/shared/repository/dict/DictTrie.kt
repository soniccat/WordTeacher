package com.aglushkov.wordteacher.shared.repository.dict

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.model.WordTeacherWord

class DictTrie: Trie<Dict.Index.Entry, DictWordData>(), Iterable<Dict.Index.Entry> {
    override fun createEntry(node: TrieNode<Dict.Index.Entry>, data: DictWordData): Dict.Index.Entry {
        return DictEntry(
            node,
            data.partOfSpeeches,
            data.indexValue,
            data.dict
        )
    }

    override fun setNodeForEntry(entry: Dict.Index.Entry, node: TrieNode<Dict.Index.Entry>) {
        (entry as DictEntry).node = node
    }
}

data class DictWordData(
    val partOfSpeeches: List<WordTeacherWord.PartOfSpeech>,
    val indexValue: Any?,
    val dict: Dict
)

class DictEntry(
    var node: TrieNode<Dict.Index.Entry>,
    partOfSpeeches: List<WordTeacherWord.PartOfSpeech>,
    indexValue: Any?,
    dict: Dict
) : Dict.Index.Entry(partOfSpeeches, indexValue, dict) {
    override val word: String
        get() {
            return node.calcWord()
        }
}
