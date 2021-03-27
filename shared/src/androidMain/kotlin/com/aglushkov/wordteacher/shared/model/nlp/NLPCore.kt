package com.aglushkov.wordteacher.shared.model.nlp

import android.content.res.Resources
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.measure
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import opennlp.tools.chunker.ChunkSample
import opennlp.tools.chunker.ChunkerME
import opennlp.tools.chunker.ChunkerModel
import opennlp.tools.lemmatizer.DictionaryLemmatizer
import opennlp.tools.postag.POSModel
import opennlp.tools.postag.POSTaggerME
import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel
import opennlp.tools.util.Span

actual class NLPCore(
    private val resources: Resources,
    private val sentenceModelRes: Int,
    private val tokenRes: Int,
    private val posModelRes: Int,
    private val lemmatizerRes: Int,
    private val chunkerRes: Int
) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val state = MutableStateFlow<Resource<NLPCore>>(Resource.Uninitialized())

    private var sentenceModel: SentenceModel? = null
    private var tokenModel: TokenizerModel? = null
    private var posModel: POSModel? = null
    private var chunkerModel: ChunkerModel? = null

    private var sentenceDetector: SentenceDetectorME? = null
    private var tokenizer: TokenizerME? = null
    private var tagger: POSTaggerME? = null
    private var lemmatizer: DictionaryLemmatizer? = null
    private var chunker: ChunkerME? = null

    init {
        // HACK: that's required to load en_pos_maxent.bin
        // https://stackoverflow.com/questions/21719266/the-profile-data-stream-has-an-invalid-format-when-using-opennlp-postagger
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
    }

    actual suspend fun waitUntilInitialized(): Resource<NLPCore> = state.first { it.isLoaded() }

    actual fun sentences(text: String) = sentenceDetector?.sentDetect(text).orEmpty().asList()
    actual fun tokenSpans(sentence: String) = tokenizer?.tokenizePos(sentence).orEmpty().asList().map {
        createTokenSpan(it)
    }
    actual fun tag(tokens: List<String>) = tagger?.tag(tokens.toTypedArray()).orEmpty().asList()
    actual fun lemmatize(tokens: List<String>, tags: List<String>) = lemmatizer?.lemmatize(tokens.toTypedArray(), tags.toTypedArray()).orEmpty().asList()
    actual fun chunk(tokens: List<String>, tags: List<String>) = chunker?.chunk(tokens.toTypedArray(), tags.toTypedArray()).orEmpty().asList()
    actual fun phrases(sentence: NLPSentence): List<PhraseSpan> =
        ChunkSample.phrasesAsSpanList(
            sentence.tokenStrings().toTypedArray(),
            sentence.tags.toTypedArray(),
            sentence.chunks.toTypedArray()
        ).map {
            createPhraseSpan(it)
        }
    // TODO: get rid of all these toTypedArray above...

    fun load() {
        state.value = Resource.Loading(this@NLPCore)
        mainScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    loadModels()
                    createMEObjects()
                }
                state.value = Resource.Loaded(this@NLPCore)
            } catch (e: Exception) {
                state.value = Resource.Error(e, true)
            }
        }
    }

    private fun loadModels() {
        val buffer = 100 * 1024
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

        Logger.measure("DictionaryLemmatizer loaded: ") {
            resources.openRawResource(lemmatizerRes).buffered(buffer).use { stream ->
                lemmatizer = DictionaryLemmatizer(stream)
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
            chunkerRes
        ).apply {
            sentenceDetector = SentenceDetectorME(this@NLPCore.sentenceModel)
            tokenizer = TokenizerME(this@NLPCore.tokenModel)
            tagger = POSTaggerME(this@NLPCore.posModel)
            chunker = ChunkerME(this@NLPCore.chunkerModel)
            lemmatizer = this@NLPCore.lemmatizer
            state.value = this@NLPCore.state.value
        }
    }

    fun createPhraseSpan(span: opennlp.tools.util.Span): PhraseSpan {
        return PhraseSpan(span.start, span.end, ChunkType.parse(span.type))
    }

    fun createTokenSpan(span: Span): TokenSpan {
        return TokenSpan(span.start, span.end)
    }
}