package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.measure
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import okio.FileSystem
import okio.Path
import opennlp.tools.chunker.ChunkSample
import opennlp.tools.chunker.ChunkerME
import opennlp.tools.chunker.ChunkerModel
import opennlp.tools.lemmatizer.DictionaryLemmatizer
import opennlp.tools.lemmatizer.Lemmatizer
import opennlp.tools.postag.POSModel
import opennlp.tools.postag.POSTaggerME
import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel
import opennlp.tools.util.Span
import opennlp.tools.util.normalizer.EmojiCharSequenceNormalizer

// TODO: share the same logic with android
actual class NLPCore(
    private val sentenceModelPath: Path,
    private val tokenPath: Path,
    private val posModelPath: Path,
    private val lemmatizerPath: Path,
    private val chunkerPath: Path,
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

    actual suspend fun waitUntilInitialized(): NLPCore {
        return state.first { it.isLoaded() }.data()!!
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

    actual suspend fun load(dispatcher: CoroutineDispatcher) {
        state.value = Resource.Loading(this@NLPCore)
        mainScope.launch {
            try {
                withContext(dispatcher) {
                    loadModels(this)
                    createMEObjects()
                }
                state.value = Resource.Loaded(this@NLPCore)
            } catch (e: Exception) {
                state.value = Resource.Error(e, true)
            }
        }
    }

    private suspend fun loadModels(context: CoroutineScope) {
        loadResource {
            Logger.measure("DictionaryLemmatizer loaded: ") {
                fileSystem.read(lemmatizerPath) {
                    MyLemmatizer(
                        this,
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
            sentenceModel = fileSystem.read(sentenceModelPath) {
                SentenceModel(inputStream())
            }
        }

        Logger.measure("TokenizerModel loaded: ") {
            tokenModel = fileSystem.read(tokenPath) {
                TokenizerModel(inputStream())
            }
        }

        Logger.measure("POSModel loaded: ") {
            posModel = fileSystem.read(posModelPath) {
                POSModel(inputStream())
            }
        }

        Logger.measure("ChunkerModel loaded: ") {
            chunkerModel = fileSystem.read(chunkerPath) {
                ChunkerModel(inputStream())
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
            sentenceModelPath,
            tokenPath,
            posModelPath,
            lemmatizerPath,
            chunkerPath,
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
