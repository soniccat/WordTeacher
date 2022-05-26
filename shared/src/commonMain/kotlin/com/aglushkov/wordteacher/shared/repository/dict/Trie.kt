package com.aglushkov.wordteacher.shared.repository.dict

import com.aglushkov.wordteacher.shared.general.extensions.addElements

//interface TrieEntry<T> {
//    //var node: TrieNode<T>
//}

abstract class Trie<T, D>: Iterable<T> {
    private val root = TrieNode<T>("", null)

    abstract fun createEntry(node: TrieNode<T>, data: D): T

    abstract fun setNodeForEntry(entry: T, node: TrieNode<T>)

    fun put(word: String, data: D) {
        val node = putWord(word)
        node.dictIndexEntries.add(
            createEntry(node, data)
        )
    }

    private fun putWord(word: String): TrieNode<T> {
        var node = root
        var innerNodeIndex = 0

        word.onEach { ch ->
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
                    val newNode = TrieNode<T>(ch.toString(), node)
                    node.children.add(newNode)
                    node = newNode
                }

                innerNodeIndex = 1

            // in the middle of prefix got that the next character is different -> split the node
            } else if (innerNodeIndex < node.prefix.length && node.prefix[innerNodeIndex] != ch) {
                val newNode1 = TrieNode<T>(
                    node.prefix.substring(innerNodeIndex, node.prefix.length),
                    node,
                    node.dictIndexEntries.toMutableList(),
                    node.children.toMutableList()
                )
                newNode1.dictIndexEntries.map {
                    //it.node = newNode1
                    setNodeForEntry(it, newNode1)
                }
                newNode1.children.onEach {
                    it.parent = newNode1
                }

                val newNode2 = TrieNode(
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
                throw RuntimeException("TrieNode.putWordInternal: impossible condition")
            }
        }

        return node
    }

    fun wordsStartWith(prefix: String, limit: Int): List<T> {
        var node: TrieNode<T>? = root
        prefix.onEach { ch ->
            node = node?.findChild(ch)
        }

        return node?.let {
            words(it, limit)
        } ?: emptyList()
    }

    private fun words(node: TrieNode<T>, limit: Int): List<T> {
        val entries = mutableListOf<T>()
        runVisitor(node) {
            entries.add(it)
            entries.size < limit
        }

        return entries
    }

    fun word(word: String): List<T> {
        var node: TrieNode<T>? = root
        word.onEach { ch ->
            node = node?.findChild(ch)
        }

        return node?.dictIndexEntries.orEmpty()
    }

    fun asSequence(): Sequence<T> {
        return sequence {
            yieldAll(iterator())
        }
    }

    fun isEmpty() = root.children.isEmpty() && root.dictIndexEntries.isEmpty()

    private fun runVisitor(
        node: TrieNode<T>,
        visitor: (entry: T) -> Boolean
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
        onFound: (node: MutableList<T>) -> Unit
    ) {
        var node: TrieNode<T>? = root
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

    private fun wordNode(word: String, startNode: TrieNode<T>?): TrieNode<T>? {
        var node: TrieNode<T>? = startNode
        word.onEach { ch ->
            node = node?.findChild(ch)

            if (node == null) return null
        }

        return node
    }

    override fun iterator(): Iterator<T> {
        return TrieIterator(root)
    }

    private class TrieIterator<T>(
        rootNode: TrieNode<T>
    ): Iterator<T> {
        private var nodeStack = ArrayDeque<TrieIteratorNode<T>>()

        init {
            if (rootNode.children.isNotEmpty() || rootNode.dictIndexEntries.isNotEmpty()) {
                walkDownUntilEntries(rootNode)
            }
        }

        private fun walkDownUntilEntries(aNode: TrieNode<T>): TrieIteratorNode<T> {
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

        override fun next(): T {
            var result: T? = null
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

        private data class TrieIteratorNode<T>(
            val node: TrieNode<T>,
            var entryIterator: Iterator<T> = node.dictIndexEntries.iterator(),
            var childIterator: Iterator<TrieNode<T>> = node.children.iterator()
        )
    }
}

open class TrieNode<T>(
    var prefix: String,
    var parent: TrieNode<T>?,
    val dictIndexEntries: MutableList<T> = mutableListOf(),
    val children: MutableList<TrieNode<T>> = mutableListOf() // TODO: add alphabetical sorting
) {
    open val isEnd: Boolean
        get() {
            return prefix.length == 1 && dictIndexEntries.isNotEmpty()
        }

    // To be able to work with nodes in this way:
    //
    //    var node: TrieNode? = n
    //    prefix.onEach { ch ->
    //        node = node?.findChild(ch)
    //    }
    //
    open fun findChild(ch: Char): TrieNode<T>? {
        if (prefix.length > 1 && prefix[1] == ch) {
            return MetaTrieNode(this, 1)
        }

        return children.firstOrNull { it.prefix.first() == ch }
    }

    fun calcWord(): String {
        return buildString {
            var node: TrieNode<T> = this@TrieNode
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

private data class MetaTrieNode<T>(
    val ref: TrieNode<T>,
    val offset: Int
): TrieNode<T>(ref.prefix, ref, ref.dictIndexEntries, ref.children) {
    override val isEnd: Boolean
        get() = offset + 1 == ref.prefix.length && ref.dictIndexEntries.isNotEmpty()

    override fun findChild(ch: Char): TrieNode<T>? {
        if (offset + 1 < prefix.length && prefix[offset + 1] == ch) {
            return MetaTrieNode(this, offset + 1)
        }

        return children.firstOrNull { it.prefix.first() == ch }
    }
}
