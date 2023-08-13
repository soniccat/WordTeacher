package com.aglushkov.wordteacher.shared

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.dsl.DslDict
import com.aglushkov.wordteacher.shared.dicts.dsl.DslIndex
import com.aglushkov.wordteacher.shared.dicts.toWordData
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.dict.DictTrie
import com.aglushkov.wordteacher.shared.repository.dict.DictWordData
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
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
        val word = DictWordData(WordTeacherWord.PartOfSpeech.Noun, 100, dict)
        trie.put("abc", word)

        val foundEntry = trie.word("abc").firstOrNull()

        assertNotNull(foundEntry)
        assertEquals(word, foundEntry.toWordData())
    }

    @Test
    fun testPut2() {
        val dict = mock<Dict>()
        val trie = DictTrie()
        val word1 = DictWordData(WordTeacherWord.PartOfSpeech.Noun, 100, dict)
        val word2 = DictWordData(WordTeacherWord.PartOfSpeech.Noun, 100, dict)
        trie.put("abc", word1)
        trie.put("abcd", word2)

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
        val word1 = DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict)
        val word2 = DictWordData(WordTeacherWord.PartOfSpeech.Noun, 100, dict)
        trie.put("abc", word1)
        trie.put("abcd", word2)

        val list = trie.asSequence().toList().map { it.word }

        assertTrue { list.contains("abc") }
        assertTrue { list.contains("abcd") }
    }

    @org.junit.Test
    fun testAsSequence2() = runTest {
        val dict = mock<Dict>()
        val trie = DictTrie()
        trie.put("drag away", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))
        trie.put("tap the barrel", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))
        trie.put("tabula rasa", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))
        trie.put("tableaux vivants", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))
        trie.put("tar and feather", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))
        trie.put("tack about", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))
        trie.put("to a proverb", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))

        val list = trie.asSequence().toList().map { it.word }
        assertEquals(7, list.size)
        assertEquals("drag away", list[0])
        assertEquals("tap the barrel", list[1])
        assertEquals("tabula rasa", list[2])
        assertEquals("tableaux vivants", list[3])
        assertEquals("tar and feather", list[4])
        assertEquals("tack about", list[5])
        assertEquals("to a proverb", list[6])
    }

    @org.junit.Test
    fun testAsSequenceWithSpace() = runTest {
        val dict = mock<Dict>()
        val trie = DictTrie()
        trie.put("tack about", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))
        trie.put("tack sth2", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))
        trie.put("drag away", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))
        trie.put("tap the barrel", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))
        trie.put("tabula rasa", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))
        trie.put("tableaux vivants", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))
        trie.put("tar and feather", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))
        trie.put("to a proverb", DictWordData(WordTeacherWord.PartOfSpeech.Noun,100, dict))

        val list = trie.wordsStartWith("tack ", 2).map { it.word }
        assertEquals(2, list.size)
        assertEquals("tack about", list[0])
        assertEquals("tack sth2", list[1])
    }
}
