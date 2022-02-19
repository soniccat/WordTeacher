package com.aglushkov.wordteacher.shared.repository.dict

import com.aglushkov.wordteacher.shared.dicts.Dict

class DictTrie {
    private val root = DictTrieNode(' ')

    fun putWord(word: String, dicIndexEntry: Dict.Index.Entry) {
        var node = root
        word.onEach { ch ->
            node = node.obtainNode(ch)
        }

        node.dictIndexEntries.add(dicIndexEntry)
    }

    fun wordsStartWith(prefix: String, limit: Int): List<Dict.Index.Entry> {
        var node: DictTrieNode? = root
        prefix.onEach { ch ->
            node = node?.children?.get(ch)
        }

        return node?.let {
            words(it, limit)
        } ?: emptyList()
    }

    private fun words(node: DictTrieNode, limit: Int): List<Dict.Index.Entry> {
        val entries = mutableListOf<Dict.Index.Entry>()
        runVisitor(node) {
            entries.add(it)
            entries.size < limit
        }

        return entries
    }

    private fun runVisitor(
        node: DictTrieNode,
        visitor: (entry: Dict.Index.Entry) -> Boolean
    ): Boolean {
        node.dictIndexEntries.onEach {
            if (!visitor.invoke(it)) return false
        }

        node.children.onEach {
            if (!runVisitor(it.value, visitor)) return false
        }

        return true
    }
}

private class DictTrieNode(
    val char: Char,
    var dictIndexEntries: MutableList<Dict.Index.Entry> = mutableListOf(),
    val children: HashMap<Char, DictTrieNode> = HashMap() // TODO: add alphabetical sorting
) {
    val isEnd: Boolean
        get() {
            return dictIndexEntries.isNotEmpty()
        }

    fun obtainNode(ch: Char): DictTrieNode {
        val node = children[ch]
        return if (node != null) {
            node
        } else {
            val newNode = DictTrieNode(ch)
            children[ch] = newNode
            newNode
        }
    }
}