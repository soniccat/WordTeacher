package com.aglushkov.wordteacher.shared

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.dsl.DslDict
import com.aglushkov.wordteacher.shared.repository.dict.DictTrie
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.mockito.kotlin.mock

class DictTrieTests {

    @Test
    fun testPut() {
        val dict = mock<Dict>()
        val trie = DictTrie()
        val entry = Dict.Index.Entry("abc", 100, dict)
        trie.putWord("abc", entry)

        val foundEntry = trie.word("abc").firstOrNull()

        assertNotNull(foundEntry)
        assertEquals(entry, foundEntry)
    }

    @Test
    fun testPut2() {
        val dict = mock<Dict>()
        val trie = DictTrie()
        val entry1 = Dict.Index.Entry("abc", 100, dict)
        val entry2 = Dict.Index.Entry("abcd", 100, dict)
        trie.putWord("abc", entry1)
        trie.putWord("abcd", entry2)

        val foundEntry1 = trie.word("abc").firstOrNull()
        val foundEntry2 = trie.word("abcd").firstOrNull()

        assertNotNull(foundEntry1)
        assertNotNull(foundEntry2)
        assertEquals(entry1, foundEntry1)
        assertEquals(entry2, foundEntry2)
    }

    @Test
    fun testAsSequence() {
        val dict = mock<Dict>()
        val trie = DictTrie()
        val entry1 = Dict.Index.Entry("abc", 100, dict)
        val entry2 = Dict.Index.Entry("abcd", 100, dict)
        trie.putWord("abc", entry1)
        trie.putWord("abcd", entry2)

        val list = trie.asSequence().toList().map { it.word }

        assertTrue { list.contains("abc") }
        assertTrue { list.contains("abcd") }
    }
}
