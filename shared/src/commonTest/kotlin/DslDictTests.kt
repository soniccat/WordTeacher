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
    fun testWordParsing() = runTest {
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

        val word = dslDict.define("term1")
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
}
