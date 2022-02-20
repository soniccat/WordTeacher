package com.aglushkov.wordteacher.shared.repository.dict

import com.aglushkov.wordteacher.shared.dicts.Dict

class DictTrie: Iterable<Dict.Index.Entry> {
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

    fun word(word: String): List<Dict.Index.Entry> {
        var node: DictTrieNode? = root
        word.onEach { ch ->
            node = node?.children?.get(ch)
        }

        return node?.dictIndexEntries.orEmpty()
    }

    fun asSequence(): Sequence<Dict.Index.Entry> {
        return sequence {
            yieldAll(iterator())
        }
    }

    fun isEmpty() = root.children.isEmpty() && root.dictIndexEntries.isEmpty()

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

    override fun iterator(): Iterator<Dict.Index.Entry> {
        return TrieIterator(root)
    }

    private class TrieIterator(
        rootNode: DictTrieNode
    ): Iterator<Dict.Index.Entry> {
        private var nodeStack = ArrayDeque<TrieIteratorNode>()

        init {
            if (rootNode.children.isNotEmpty() || rootNode.dictIndexEntries.isNotEmpty()) {
                walkDownUntilEntries(rootNode)
            }
        }

        private fun walkDownUntilEntries(aNode: DictTrieNode): TrieIteratorNode {
            //nodeStack.addLast(anIteratorNode)
            //if (anIteratorNode.entryIterator.hasNext()) return anIteratorNode.node

            var node = aNode
            do {
                val iteratorNode = TrieIteratorNode(node)
                nodeStack.addLast(iteratorNode)

                if (iteratorNode.entryIterator.hasNext()) {
                    return iteratorNode
                } else if (iteratorNode.childIterator.hasNext()) {
                    node = iteratorNode.childIterator.next().value
                } else {
                    break
                }
            } while (true)
//            {
//                nodeIterator = TrieIteratorNode(nodeIterator.childIterator.next().value)
//                nodeStack.addLast(nodeIterator)
//
//                if (nodeIterator.entryIterator.hasNext()) {
//                    return nodeIterator.node
//                }
//            }

            throw RuntimeException("TrieIterator.walkDownUntilEntries returns null")
        }

        override fun hasNext(): Boolean = nodeStack.lastOrNull() != null &&
                (nodeStack.last().entryIterator.hasNext() || nodeStack.last().childIterator.hasNext())

        override fun next(): Dict.Index.Entry {
            var result: Dict.Index.Entry? = null
            val entry = nodeStack.last()
            if (entry.entryIterator.hasNext()) {
                result = entry.entryIterator.next()
            }

            if (entry.childIterator.hasNext()) {
                val nextLastNode = walkDownUntilEntries(entry.childIterator.next().value)
                result = nextLastNode.entryIterator.next()
            }

            while (nodeStack.isNotEmpty() &&
                !nodeStack.last().entryIterator.hasNext() &&
                !nodeStack.last().childIterator.hasNext()) {
                nodeStack.removeLast()
            }

            if (result != null) {
                return result
            }

            throw RuntimeException("TrieIterator.next returns null")
        }

        private data class TrieIteratorNode(
            val node: DictTrieNode,
            var entryIterator: Iterator<Dict.Index.Entry> = node.dictIndexEntries.iterator(),
            var childIterator: Iterator<Map.Entry<Char, DictTrieNode>> = node.children.iterator()
        )
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