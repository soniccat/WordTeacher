package com.aglushkov.wordteacher.shared

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.dsl.DslDict
import com.aglushkov.wordteacher.shared.dicts.toWordData
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.dict.DictTrie
import com.aglushkov.wordteacher.shared.repository.dict.DictWordData
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
        val word = DictWordData("abc", WordTeacherWord.PartOfSpeech.Noun, 100, dict)
        trie.putWord(word)

        val foundEntry = trie.word("abc").firstOrNull()

        assertNotNull(foundEntry)
        assertEquals(word, foundEntry.toWordData())
    }

    @Test
    fun testPut2() {
        val dict = mock<Dict>()
        val trie = DictTrie()
        val word1 = DictWordData("abc", WordTeacherWord.PartOfSpeech.Noun, 100, dict)
        val word2 = DictWordData("abcd", WordTeacherWord.PartOfSpeech.Noun, 100, dict)
        trie.putWord(word1)
        trie.putWord(word2)

        val foundEntry1 = trie.word("abc").firstOrNull()
        val foundEntry2 = trie.word("abcd").firstOrNull()

        assertNotNull(foundEntry1)
        assertNotNull(foundEntry2)
        assertEquals(word1, foundEntry1.toWordData())
        assertEquals(word2, foundEntry2.toWordData())
    }

    @Test
    fun testAsSequence() {
        val dict = mock<Dict>()
        val trie = DictTrie()
        val word1 = DictWordData("abc", WordTeacherWord.PartOfSpeech.Noun,100, dict)
        val word2 = DictWordData("abcd", WordTeacherWord.PartOfSpeech.Noun, 100, dict)
        trie.putWord(word1)
        trie.putWord(word2)

        val list = trie.asSequence().toList().map { it.word }

        assertTrue { list.contains("abc") }
        assertTrue { list.contains("abcd") }
    }
}
