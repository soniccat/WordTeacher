package com.aglushkov.wordteacher.shared

import com.aglushkov.wordteacher.shared.dicts.Language
import com.aglushkov.wordteacher.shared.dicts.dsl.DslDict
import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem


class DslDictTests {

    @Test
    fun testHeaderParsing() = runTest {
        val fakeFileSystem = FakeFileSystem()
        val dirPath = "/test".toPath()
        val dictPath = dirPath.div("dict.dsl")
        fakeFileSystem.createDirectories(dirPath)
        fakeFileSystem.write(dictPath, true) {
            writeUtf8(
                """
                #NAME	"testname"
                #INDEX_LANGUAGE	"English"
                #CONTENTS_LANGUAGE	"Russian"
                """.trimIndent()
            )
        }

        val dslDict = DslDict(dictPath, fakeFileSystem)
        dslDict.load()

        assertTrue { dslDict.name == "testname" }
        assertTrue { dslDict.fromLang == Language.EN }
        assertTrue { dslDict.toLang == Language.RU }
    }

    @Test
    fun testWordParsingOneWord() = runTest {
        val fakeFileSystem = FakeFileSystem()
        val dirPath = "/test".toPath()
        val dictPath = dirPath.div("dict.dsl")
        fakeFileSystem.createDirectories(dirPath)
        fakeFileSystem.write(dictPath, true) {
            writeUtf8(
                """
                #NAME	"testname"
                #INDEX_LANGUAGE	"English"
                #CONTENTS_LANGUAGE	"Russian"

                term1
                	[m1]1) [trn]def1[/trn][/m]
                	[m1]2) [trn]def2[/trn][/m]
                	[m1]3) [trn]def3 [com]([i]comment3[/i])[/com][/trn][/m]
                	[m1]4) [trn]def4[/trn][/m]
                	[m1][*]•[/*][/m]
                	[m1][*][ex][lang id=1033]ex1[/lang] — ex1_1[/ex][/*][/m]
                	[m1][*]- [ref]ref[/ref]
                """.trimIndent()
            )
        }

        val dslDict = DslDict(dictPath, fakeFileSystem)
        dslDict.load()

        val word = dslDict.define("term1").firstOrNull()
        assertNotNull(word)
        assertEquals(
            WordTeacherWord(
                word = "term1",
                transcription = null,
                definitions = mapOf(
                    WordTeacherWord.PartOfSpeech.Undefined to listOf(
                        WordTeacherDefinition(
                            definitions = listOf("def1", "def2", "def3 (comment3)", "def4"),
                            examples = listOf("ex1 — ex1_1"),
                            synonyms = listOf(),
                            imageUrl = null
                        )
                    )
                ),
                types = emptyList()
            ),
            word
        )
    }

    @Test
    fun testWordParsingWithTwoDefs() = runTest {
        val fakeFileSystem = FakeFileSystem()
        val dirPath = "/test".toPath()
        val dictPath = dirPath.div("dict.dsl")
        fakeFileSystem.createDirectories(dirPath)
        fakeFileSystem.write(dictPath, true) {
            writeUtf8(
                """
                #NAME	"testname"
                #INDEX_LANGUAGE	"English"
                #CONTENTS_LANGUAGE	"Russian"
                
                another term
                	\[[t]transcription[/t]\]
                	[p]phrasal verb[/p]
                	[m1]1) [trn]def1[/trn][/m]
                	[m2][*][ex][lang id=1033]ex1[/lang] — ex1_1[/ex][/*][/m]
                	[m1]2) [trn]def2 [i][com](def2_1)[/com][/trn][/i][/m]
                	[m2][*][ex][lang id=1033]ex2[/lang] — ex2_1[/ex][/*][/m]
                """.trimIndent()
            )
        }

        val dslDict = DslDict(dictPath, fakeFileSystem)
        dslDict.load()

        val word = dslDict.define("another term").firstOrNull()
        assertNotNull(word)
        assertEquals(
            WordTeacherWord(
                word = "another term",
                transcription = "transcription",
                definitions = mapOf(
                    WordTeacherWord.PartOfSpeech.Undefined to listOf(
                        WordTeacherDefinition(
                            definitions = listOf("def1"),
                            examples = listOf("ex1 — ex1_1"),
                            synonyms = listOf(),
                            imageUrl = null
                        ),
                        WordTeacherDefinition(
                            definitions = listOf("def2 (def2_1)"),
                            examples = listOf("ex2 — ex2_1"),
                            synonyms = listOf(),
                            imageUrl = null
                        )
                    )
                ),
                types = emptyList()
            ),
            word
        )
    }

    @Test
    fun testTwoWordParsing() = runTest {
        val fakeFileSystem = FakeFileSystem()
        val dirPath = "/test".toPath()
        val dictPath = dirPath.div("dict.dsl")
        fakeFileSystem.createDirectories(dirPath)
        fakeFileSystem.write(dictPath, true) {
            writeUtf8(
                """
                #NAME	"testname"
                #INDEX_LANGUAGE	"English"
                #CONTENTS_LANGUAGE	"Russian"
                
                term1
                	\[[t]transcription[/t]\]
                	[p]phrasal verb[/p]
                	[m1]1) [trn]def1[/trn][/m]
                	[m2][*][ex][lang id=1033]ex1[/lang] — ex1_1[/ex][/*][/m]

                term2
                	\[[t]transcription2[/t]\]
                	[p]phrasal verb2[/p]
                	[m1]1) [trn]def2[/trn][/m]
                	[m2][*][ex][lang id=1033]ex2[/lang] — ex2_2[/ex][/*][/m]
                """.trimIndent()
            )
        }

        val dslDict = DslDict(dictPath, fakeFileSystem)
        dslDict.load()

        val word1 = dslDict.define("term1").firstOrNull()
        val word2 = dslDict.define("term2").firstOrNull()
        assertNotNull(word1)
        assertNotNull(word2)
        assertEquals(
            WordTeacherWord(
                word = "term1",
                transcription = "transcription",
                definitions = mapOf(
                    WordTeacherWord.PartOfSpeech.Undefined to listOf(
                        WordTeacherDefinition(
                            definitions = listOf("def1"),
                            examples = listOf("ex1 — ex1_1"),
                            synonyms = listOf(),
                            imageUrl = null
                        )
                    )
                ),
                types = emptyList()
            ),
            word1
        )
        assertEquals(
            WordTeacherWord(
                word = "term2",
                transcription = "transcription2",
                definitions = mapOf(
                    WordTeacherWord.PartOfSpeech.Undefined to listOf(
                        WordTeacherDefinition(
                            definitions = listOf("def2"),
                            examples = listOf("ex2 — ex2_2"),
                            synonyms = listOf(),
                            imageUrl = null
                        )
                    )
                ),
                types = emptyList()
            ),
            word2
        )
    }
}
