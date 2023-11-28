package com.aglushkov.wordteacher.shared

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.dsl.DslIndex
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.Test
import org.mockito.kotlin.mock

class DslIndexTests {
    @Test
    fun testSaveAndLoadThreeTerms() = runTest {
        val fakeFileSystem = FakeFileSystem()
        val dirPath = "/test".toPath()
        val dictPath = dirPath.div("dict.dsl")
        val dict = mock<Dict>()
        fakeFileSystem.createDirectories(dirPath)

        val indexPath = (dictPath.toString() + "index").toPath()
        val dslIndex = DslIndex(dict, indexPath, fakeFileSystem)
        dslIndex.add("term1", WordTeacherWord.PartOfSpeech.Adjective, 100)
        dslIndex.add("term2", WordTeacherWord.PartOfSpeech.Noun, 101)
        dslIndex.add("term3", WordTeacherWord.PartOfSpeech.Adverb, 102)
        dslIndex.save()

        val dslIndex2 = DslIndex(dict, indexPath, fakeFileSystem)
        assertEquals(100, dslIndex2.offset("term1"))
        assertEquals(101, dslIndex2.offset("term2"))
        assertEquals(102, dslIndex2.offset("term3"))
        assertEquals(WordTeacherWord.PartOfSpeech.Adjective, dslIndex2.partOfSpeech("term1"))
        assertEquals(WordTeacherWord.PartOfSpeech.Noun, dslIndex2.partOfSpeech("term2"))
        assertEquals(WordTeacherWord.PartOfSpeech.Adverb, dslIndex2.partOfSpeech("term3"))
    }
}
