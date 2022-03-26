import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.shared.dicts.dsl.DslDict
import com.aglushkov.wordteacher.shared.features.article.vm.DictAnnotationResolver
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DictAnnotationResolverTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testPhrasalVerb() {
        val testDispatcher = StandardTestDispatcher()

        val res = RuntimeEnvironment.getApplication().resources
        val nlpCore = NLPCore(
            res,
            R.raw.en_sent,
            R.raw.en_token,
            R.raw.en_pos_maxent,
            R.raw.en_lemmatizer_dict,
            R.raw.en_chunker,
            testDispatcher
        )
        nlpCore.load(testDispatcher)
        testDispatcher.scheduler.runCurrent()

        val nlpSentenceProcessor = NLPSentenceProcessor(
            nlpCore
        )

        val fakeFileSystem = FakeFileSystem()
        val dirPath = "/test".toPath()
        val dictPath = dirPath.div("dict.dsl")
        fakeFileSystem.createDirectories(dirPath)
        fakeFileSystem.write(dictPath, true) {
            writeUtf8(
                """
                make up
                	[m1]1) [trn]def1[/trn][/m]
                """.trimIndent()
            )
        }

        val dict = DslDict(dictPath, fakeFileSystem)
        runBlocking {
            dict.load()
        }

        val nlpSentence = NLPSentence(
            text = "I've made up this idea"
        )
        nlpSentenceProcessor.process(nlpSentence)

        val dictAnnotationResolver = DictAnnotationResolver()
        val annotations = dictAnnotationResolver.resolve(
            listOf(dict),
            nlpSentence,
            nlpSentence.phrases()
        )

        assertEquals(1, annotations.size)
        assertEquals("make up", annotations.first().entry.word)
    }
}