package com.aglushkov.wordteacher.shared.repository.dict

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.model.WordTeacherWord

class DictTrie: Iterable<Dict.Index.Entry> {
    private val root = DictTrieNode(' ', null)

    fun putWord(data: DictWordData) {
        var node = root
        data.word.onEach { ch ->
            node = node.obtainNode(ch)
        }

        node.dictIndexEntries.add(
            DictEntry(
                node,
                data.partOfSpeech,
                data.indexValue,
                data.dict
            )
        )
    }

    fun wordsStartWith(prefix: String, limit: Int): List<Dict.Index.Entry> {
        var node: DictTrieNode? = root
        prefix.onEach { ch ->
            node = node?.findChild(ch)
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
            node = node?.findChild(ch)
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
            if (!runVisitor(it, visitor)) return false
        }

        return true
    }

    fun entry(
        word: String,
        nextWord: (needAnotherOne: Boolean) -> String?,
        onFound: (node: MutableList<Dict.Index.Entry>) -> Unit
    ) {
        var node: DictTrieNode? = root
        word.onEach { ch ->
            node = node?.findChild(ch)
            if (node == null) return
        }

        val spaceNode = node?.findChild(' ')
        if (spaceNode != null) {
            node = spaceNode
        } else {
            return
        }

        var needAnotherOne = false
        var nw = nextWord(needAnotherOne)
        while (nw != null) {
            val nextNode = wordNode(nw, node)
            if (nextNode == null) {
                needAnotherOne = true
            } else {
                needAnotherOne = false

                if (nextNode.isEnd) {
                    onFound(nextNode.dictIndexEntries)
                }

                val spaceNode2 = nextNode.findChild(' ')
                if (spaceNode2 != null) {
                    node = spaceNode2
                } else if (nextNode.dictIndexEntries.isEmpty()) {
                    needAnotherOne = true
                } else {
                    //return nextNode.dictIndexEntries
                    node = nextNode
                    break
                }
            }

            nw = nextWord(needAnotherOne)
        }

        val safeNode = node
        if (safeNode != null && safeNode.isEnd) {
            onFound(safeNode.dictIndexEntries)
        }
    }

    private fun wordNode(word: String, startNode: DictTrieNode?): DictTrieNode? {
        var node: DictTrieNode? = startNode
        word.onEach { ch ->
            node = node?.findChild(ch)

            if (node == null) return null
        }

        return node
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
            var node = aNode
            do {
                val iteratorNode = TrieIteratorNode(node)
                nodeStack.addLast(iteratorNode)

                if (iteratorNode.entryIterator.hasNext()) {
                    return iteratorNode
                } else if (iteratorNode.childIterator.hasNext()) {
                    node = iteratorNode.childIterator.next()
                } else {
                    break
                }
            } while (true)

            throw RuntimeException("TrieIterator.walkDownUntilEntries returns null")
        }

        override fun hasNext(): Boolean = nodeStack.lastOrNull() != null &&
                (nodeStack.last().entryIterator.hasNext() || nodeStack.last().childIterator.hasNext())

        override fun next(): Dict.Index.Entry {
            var result: Dict.Index.Entry? = null
            val entry = nodeStack.last()
            if (entry.entryIterator.hasNext()) {
                result = entry.entryIterator.next()

            } else if (entry.childIterator.hasNext()) {
                val nextLastNode = walkDownUntilEntries(entry.childIterator.next())
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
            var childIterator: Iterator<DictTrieNode> = node.children.iterator()
        )
    }

    // For debugging

    fun singleNodeCount(): Int {
        return singleNodeCount(root)
    }

    private fun singleNodeCount(node: DictTrieNode): Int {
        var c = 0;

        if (node.children.size == 1 && node.dictIndexEntries.size == 0) {
            c = 1
        }

        node.children.onEach {
            c += singleNodeCount(it)
        }

        return c
    }
}

data class DictWordData(
    val word: String,
    val partOfSpeech: WordTeacherWord.PartOfSpeech,
    val indexValue: Any?,
    val dict: Dict
)

private class DictEntry(
    private val node: DictTrieNode,
    partOfSpeech: WordTeacherWord.PartOfSpeech,
    indexValue: Any?,
    dict: Dict
) : Dict.Index.Entry(partOfSpeech, indexValue, dict) {
    override val word: String
        get() {
            return node.calcWord()
        }
}

private class DictTrieNode(
    val ch: Char,
    val parent: DictTrieNode?,
    val dictIndexEntries: MutableList<Dict.Index.Entry> = mutableListOf(),
    val children: MutableList<DictTrieNode> = mutableListOf() // TODO: add alphabetical sorting
) {
    val isEnd: Boolean
        get() {
            return dictIndexEntries.isNotEmpty()
        }

    fun obtainNode(ch: Char): DictTrieNode {
        val node = findChild(ch)
        return if (node != null) {
            node
        } else {
            val newNode = DictTrieNode(ch, this)
            children.add(newNode)
            newNode
        }
    }

    fun findChild(ch: Char): DictTrieNode? {
        return children.firstOrNull { it.ch == ch }
    }

    fun calcWord(): String {
        return buildString {
            var node: DictTrieNode = this@DictTrieNode
            do {
                insert(0, node.ch)

                val p = node.parent
                if (p != null) {
                    node = p
                } else {
                    break;
                }
            } while (node.parent != null)
        }
    }
}
