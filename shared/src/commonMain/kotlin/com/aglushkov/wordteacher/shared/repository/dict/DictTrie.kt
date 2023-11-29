package com.aglushkov.wordteacher.shared.repository.dict

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.model.WordTeacherWord

class DictTrie: Trie<Dict.Index.Entry, DictWordData>(), Iterable<Dict.Index.Entry> {
    override fun createEntry(node: TrieNode<Dict.Index.Entry>, data: DictWordData): Dict.Index.Entry {
        return DictEntry(
            node,
            data.partOfSpeech,
            data.indexValue,
            data.dict
        )
    }

    override fun setNodeForEntry(entry: Dict.Index.Entry, node: TrieNode<Dict.Index.Entry>) {
        (entry as DictEntry).node = node
    }

    override fun getNodeFromEntry(entry: Dict.Index.Entry): TrieNode<Dict.Index.Entry> {
        return (entry as DictEntry).node
    }
}

data class DictWordData(
    val partOfSpeech: WordTeacherWord.PartOfSpeech,
    val indexValue: Any?,
    val dict: Dict
)

class DictEntry(
    var node: TrieNode<Dict.Index.Entry>,
    partOfSpeech: WordTeacherWord.PartOfSpeech,
    indexValue: Any?,
    dict: Dict
) : Dict.Index.Entry(partOfSpeech, indexValue, dict) {
    override val word: String
        get() {
            return node.calcWord()
        }
}
