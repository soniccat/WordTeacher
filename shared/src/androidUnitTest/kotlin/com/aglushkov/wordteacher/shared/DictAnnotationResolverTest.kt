package com.aglushkov.wordteacher.shared

import com.aglushkov.wordteacher.android.R
import com.aglushkov.wordteacher.shared.features.article.vm.DictAnnotationResolver
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
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
        "lemmatizer_test".toPath(),
        FakeFileSystem(),
        testDispatcher
    )
    val nlpSentenceProcessor = NLPSentenceProcessor(
        nlpCore
    )
    val dictAnnotationResolver = DictAnnotationResolver()

    init {
        nlpCore.load(testDispatcher)
        testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun testPhrasalVerb() {
        val dict = createFakeDict(
            buildDictContent {
                addTerm("make up", listOf("def1"))
            }
        )

        val nlpSentence = nlpSentenceProcessor.processString("I've made up this idea")
        val annotations = dictAnnotationResolver.resolve(
            listOf(dict),
            nlpSentence,
            nlpSentence.phrases()
        )

        assertEquals(1, annotations.size)
        assertEquals("make up", annotations.first().entry.word)
    }

    @Test
    fun testPhrasalVerbWithNounPhrase() {
        val dict = createFakeDict(
            buildDictContent {
                addTerm("talk into", listOf("def1"))
            }
        )

        val nlpSentence = nlpSentenceProcessor.processString("He have talked my best friend into it")
        val annotations = dictAnnotationResolver.resolve(
            listOf(dict),
            nlpSentence,
            nlpSentence.phrases()
        )

        assertEquals(1, annotations.size)
        assertEquals("talk into", annotations.first().entry.word)
    }

    @Test
    fun testPhrasalVerbBeOn() {
        val dict = createFakeDict(
            buildDictContent {
                addTerm("be on", listOf("def1"))
            }
        )

        val nlpSentence = nlpSentenceProcessor.processString("In fact, many phrasal verbs are distinct variations on the same base verb, which can add to the confusion")
        val annotations = dictAnnotationResolver.resolve(
            listOf(dict),
            nlpSentence,
            nlpSentence.phrases()
        )

        assertEquals(0, annotations.size)
    }

    @Test
    fun testTwoPhrasalVerbsGetOver() {
        val dict = createFakeDict(
            buildDictContent {
                addTerm("get over", listOf("def1"))
                addTerm("get over with", listOf("def1"))
            }
        )

        val nlpSentence = nlpSentenceProcessor.processString("Let’s look at the phrasal verb get over as an example")
        val annotations = dictAnnotationResolver.resolve(
            listOf(dict),
            nlpSentence,
            nlpSentence.phrases()
        )

        assertEquals(1, annotations.size)
        assertEquals("get over", annotations.first().entry.word)
    }

    @Test
    fun testPhrasalVerbPickUpAtTheBeginning() {
        val dict = createFakeDict(
            buildDictContent {
                addTerm("pick up", listOf("def1"))
            }
        )

        val nlpSentence = nlpSentenceProcessor.processString("Pick it up and carry it to the kitchen.")
        val annotations = dictAnnotationResolver.resolve(
            listOf(dict),
            nlpSentence,
            nlpSentence.phrases()
        )

        assertEquals(1, annotations.size)
        assertEquals("pick up", annotations.first().entry.word)
    }

    @Test
    fun testPhraseHighlight() {
        val dict = createFakeDict(
            buildDictContent {
                addTerm("much as", listOf("def1"))
            }
        )

        val nlpSentence = nlpSentenceProcessor.processString("Phrasal verbs are two or more words that together act as a completely new word, with a meaning separate from the original words")
        val annotations = dictAnnotationResolver.resolve(
            listOf(dict),
            nlpSentence,
            nlpSentence.phrases()
        )

        assertEquals(0, annotations.size)
    }

    @Test
    fun testNountHighlight() {
        val dict = createFakeDict(
            buildDictContent {
                addTerm("verb", listOf("def1"))
            }
        )

        val nlpSentence = nlpSentenceProcessor.processString("Phrasal verbs are two or more words")
        val annotations = dictAnnotationResolver.resolve(
            listOf(dict),
            nlpSentence,
            nlpSentence.phrases()
        )

        assertEquals(1, annotations.size)
    }
}
