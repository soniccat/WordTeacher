package com.aglushkov.wordteacher.shared

import com.aglushkov.wordteacher.android.R
import com.aglushkov.wordteacher.shared.features.article.vm.DictAnnotationResolver
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class DictAnnotationResolverTest {

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
    val nlpSentenceProcessor = NLPSentenceProcessor(
        nlpCore
    )

    init {
        nlpCore.load(testDispatcher)
        testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun testPhrasalVerb() {
        val dict = createFakeDict(
            """
            make up
            	[m1]1) [trn]def1[/trn][/m]
            """.trimIndent()
        )
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

