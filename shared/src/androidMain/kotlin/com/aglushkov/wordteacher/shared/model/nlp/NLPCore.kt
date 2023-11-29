package com.aglushkov.wordteacher.shared.model.nlp

import android.content.res.Resources
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.measure
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.source
import opennlp.tools.chunker.ChunkSample
import opennlp.tools.chunker.ChunkerME
import opennlp.tools.chunker.ChunkerModel
import opennlp.tools.lemmatizer.Lemmatizer
import opennlp.tools.postag.POSModel
import opennlp.tools.postag.POSTaggerME
import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel
import opennlp.tools.util.Span
import opennlp.tools.util.normalizer.EmojiCharSequenceNormalizer

actual class NLPCore(
    private val resources: Resources,
    private val sentenceModelRes: Int,
    private val tokenRes: Int,
    private val posModelRes: Int,
    private val lemmatizerRes: Int,
    private val chunkerRes: Int,
    private val nlpPath: Path,
    private val fileSystem: FileSystem,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val mainScope = CoroutineScope(dispatcher + SupervisorJob())

    private val lemmatizerState = MutableStateFlow<Resource<MyLemmatizer>>(Resource.Uninitialized())
    private val state = MutableStateFlow<Resource<NLPCore>>(Resource.Uninitialized())

    private var sentenceModel: SentenceModel? = null
    private var tokenModel: TokenizerModel? = null
    private var posModel: POSModel? = null
    private var chunkerModel: ChunkerModel? = null

    private var sentenceDetector: SentenceDetectorME? = null
    private var tokenizer: TokenizerME? = null
    private var tagger: POSTaggerME? = null
    private var lemmatizer: MyLemmatizer? = null
    private var chunker: ChunkerME? = null

    init {
        // HACK: that's required to load en_pos_maxent.bin
        // https://stackoverflow.com/questions/21719266/the-profile-data-stream-has-an-invalid-format-when-using-opennlp-postagger
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
    }

    actual suspend fun waitUntilInitialized(): NLPCore {
        return state.first { it.isLoaded() }.data()!!
    }

    fun isInitialized(): Boolean {
        return state.value.isLoaded()
    }

    actual suspend fun waitUntilLemmatizerInitialized(): NLPLemmatizer {
        return lemmatizerState.first{ it.isLoaded() }.data()!!
    }

    actual fun normalizeText(text: String): String {
        return EmojiCharSequenceNormalizer.getInstance().normalize(text).toString()
    }

    actual fun sentenceSpans(text: String): List<SentenceSpan> = sentenceDetector?.sentPosDetect(text).orEmpty().flatMap {
        val subSequence = text.subSequence(it.start, it.end)
        // fixing span detecting for direct speech when a sentence ends with "\n
        /* Example:
        “All too true.” Clover nodded sadly. “And an inadequate response in affairs of state, I think it’s fair to say.”
        “In whats o’ what?” mumbled Downside, baffled.
         */
        val splitIndex = subSequence.indexOf("\"\n")
        if (splitIndex == -1) {
            listOf(SentenceSpan(it.start, it.end))
        } else {
            listOf(SentenceSpan(it.start, it.start + splitIndex + 1), SentenceSpan(it.start + splitIndex + 2, it.end))
        }
    }
    actual fun tokenSpans(sentence: String) = tokenizer?.tokenizePos(
        sentence
    ).orEmpty().asList().map {
        createTokenSpan(it)
    }
    actual fun tag(tokens: List<String>) = tagger?.tag(tokens.toTypedArray()).orEmpty().asList()
    actual fun lemmatize(tokens: List<String>, tags: List<String>) = lemmatizer?.lemmatize(tokens, tags).orEmpty().asList()
    actual fun chunk(tokens: List<String>, tags: List<String>) = chunker?.chunk(tokens.toTypedArray(), tags.toTypedArray()).orEmpty().asList()

    suspend fun load(dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        loadResource {
            withContext(dispatcher) {
                loadModels(this)
                createMEObjects()
            }
            this@NLPCore
        }.collect(state)
    }

    private suspend fun loadModels(scope: CoroutineScope) {
        val buffer = 100 * 1024

        loadResource {
            Logger.measure("DictionaryLemmatizer loaded: ") {
                resources.openRawResource(lemmatizerRes).buffered(buffer).use { inputStream ->
                    MyLemmatizer(
                        inputStream.source(),
                        nlpPath,
                        fileSystem
                    ).apply {
                        load()
                    }.also {
                        lemmatizer = it
                    }
                }
            }
        }.collect(lemmatizerState)

        Logger.measure("SentenceModel loaded: ") {
            resources.openRawResource(sentenceModelRes).buffered(buffer).use { modelIn ->
                sentenceModel = SentenceModel(modelIn)
            }
        }

        Logger.measure("TokenizerModel loaded: ") {
            resources.openRawResource(tokenRes).buffered(buffer).use { stream ->
                tokenModel = TokenizerModel(stream)
            }
        }

        Logger.measure("POSModel loaded: ") {
            resources.openRawResource(posModelRes).buffered(buffer).use { stream ->
                posModel = POSModel(stream)
            }
        }

        Logger.measure("ChunkerModel loaded: ") {
            resources.openRawResource(chunkerRes).buffered(buffer).use { stream ->
                chunkerModel = ChunkerModel(stream)
            }
        }
    }

    private fun createMEObjects() {
        sentenceDetector = SentenceDetectorME(sentenceModel)
        tokenizer = TokenizerME(tokenModel)
        tagger = POSTaggerME(posModel)
        chunker = ChunkerME(chunkerModel)
    }

    // to be able to work with NLPCore in a separate thread
    actual fun clone(): NLPCore {
        assert(state.value.isLoaded()) { "Can't copy NLPCore when it isn't loaded" }

        return NLPCore(
            resources,
            sentenceModelRes,
            tokenRes,
            posModelRes,
            lemmatizerRes,
            chunkerRes,
            nlpPath,
            fileSystem
        ).apply {
            sentenceDetector = SentenceDetectorME(this@NLPCore.sentenceModel)
            tokenizer = TokenizerME(this@NLPCore.tokenModel)
            tagger = POSTaggerME(this@NLPCore.posModel)
            chunker = ChunkerME(this@NLPCore.chunkerModel)
            lemmatizer = this@NLPCore.lemmatizer
            state.value = this@NLPCore.state.value
        }
    }

    fun createTokenSpan(span: Span): TokenSpan {
        return TokenSpan(span.start, span.end)
    }
}

actual fun phrasesAsSpanList(
    tokenStrings: List<String>, tags: List<String>, chunks: List<String>
): List<PhraseSpan> = ChunkSample.phrasesAsSpanList(
    tokenStrings.toTypedArray(),
    tags.toTypedArray(),
    chunks.toTypedArray()
).map {
    createPhraseSpan(it)
}

private fun createPhraseSpan(span: Span): PhraseSpan {
    return PhraseSpan(span.start, span.end, chunkEnum(span.type))
}