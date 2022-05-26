package com.aglushkov.wordteacher.shared.repository.dict

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.extensions.addElements
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.WordTeacherWord

class DictTrie: Iterable<Dict.Index.Entry> {
    private val root = DictTrieNode("", null)

    fun putWord(data: DictWordData) {
        val node = putWordInternal(data)
        node.dictIndexEntries.add(
            DictEntry(
                node,
                data.partOfSpeech,
                data.indexValue,
                data.dict
            )
        )
    }

    private fun putWordInternal(data: DictWordData): DictTrieNode {
        var node = root
        var innerNodeIndex = 0

        data.word.onEach { ch ->
            // match with prefix -> go along the prefix
            if (innerNodeIndex < node.prefix.length && node.prefix[innerNodeIndex] == ch) {
                innerNodeIndex += 1

            // reached the end of prefix and there aren't any children aren't any entries -> extend prefix
            // skip if node is root
            } else if (node != root && node.prefix.length == innerNodeIndex && node.children.isEmpty() && node.dictIndexEntries.isEmpty()) {
                node.prefix = node.prefix + ch
                innerNodeIndex += 1

            // reached the end of prefix and there children or entries -> try to find a child with the same prefix or add a new child
            } else if (node.prefix.length == innerNodeIndex && (node.children.isNotEmpty() || node.dictIndexEntries.isNotEmpty() || node == root)) {
                val childNode = node.children.firstOrNull {
                    it.prefix.first() == ch
                }

                if (childNode != null) {
                    node = childNode
                } else {
                    val newNode = DictTrieNode(ch.toString(), node)
                    node.children.add(newNode)
                    node = newNode
                }

                innerNodeIndex = 1

            // in the middle of prefix got that the next character is different -> split the node
            } else if (innerNodeIndex < node.prefix.length && node.prefix[innerNodeIndex] != ch) {
                val newNode1 = DictTrieNode(
                    node.prefix.substring(innerNodeIndex, node.prefix.length),
                    node,
                    node.dictIndexEntries.toMutableList(),
                    node.children.toMutableList()
                )
                newNode1.dictIndexEntries.map {
                    (it as DictEntry).node = newNode1
                }
                newNode1.children.onEach {
                    it.parent = newNode1
                }

                val newNode2 = DictTrieNode(
                    ch.toString(),
                    node
                )

                node.prefix = node.prefix.substring(0, innerNodeIndex)
                node.dictIndexEntries.clear()
                node.children.clear()
                node.children.addElements(newNode1, newNode2)

                node = newNode2
                innerNodeIndex = 1
            } else {
                throw RuntimeException("DictTrie.putWordInternal: impossible condition")
            }
        }

        return node
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
                } else if (nextNode.isEnd && nextNode.dictIndexEntries.isEmpty()) {
                    needAnotherOne = true
                } else {
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
}

data class DictWordData(
    val word: String,
    val partOfSpeech: WordTeacherWord.PartOfSpeech,
    val indexValue: Any?,
    val dict: Dict
)

private class DictEntry(
    var node: DictTrieNode,
    partOfSpeech: WordTeacherWord.PartOfSpeech,
    indexValue: Any?,
    dict: Dict
) : Dict.Index.Entry(partOfSpeech, indexValue, dict) {
    override val word: String
        get() {
            return node.calcWord()
        }
}

private open class DictTrieNode(
    var prefix: String,
    var parent: DictTrieNode?,
    val dictIndexEntries: MutableList<Dict.Index.Entry> = mutableListOf(),
    val children: MutableList<DictTrieNode> = mutableListOf() // TODO: add alphabetical sorting
) {
    open val isEnd: Boolean
        get() {
            return prefix.length == 1 && dictIndexEntries.isNotEmpty()
        }

    // To be able to work with nodes in this way:
    //
    //    var node: DictTrieNode? = n
    //    prefix.onEach { ch ->
    //        node = node?.findChild(ch)
    //    }
    //
    open fun findChild(ch: Char): DictTrieNode? {
        if (prefix.length > 1 && prefix[1] == ch) {
            return MetaDictTrieNode(this, 1)
        }

        return children.firstOrNull { it.prefix.first() == ch }
    }

    fun calcWord(): String {
        return buildString {
            var node: DictTrieNode = this@DictTrieNode
            while (true) {
                insert(0, node.prefix)

                val p = node.parent
                if (p != null) {
                    node = p
                } else {
                    break;
                }
            }
        }
    }
}

private data class MetaDictTrieNode(
    val ref: DictTrieNode,
    val offset: Int
): DictTrieNode(ref.prefix, ref, ref.dictIndexEntries, ref.children) {
    override val isEnd: Boolean
        get() = offset + 1 == ref.prefix.length && ref.dictIndexEntries.isNotEmpty()

    override fun findChild(ch: Char): DictTrieNode? {
        if (offset + 1 < prefix.length && prefix[offset + 1] == ch) {
            return MetaDictTrieNode(this, offset + 1)
        }

        return children.firstOrNull { it.prefix.first() == ch }
    }
}
