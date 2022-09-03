package com.aglushkov.wordteacher.shared.features.article.vm

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsContext
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsWordContext
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.Quadruple
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.nlp.ChunkType
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.split
import com.aglushkov.wordteacher.shared.model.nlp.toPartOfSpeech
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardsRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.russhwolf.settings.coroutines.FlowSettings
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import io.ktor.util.Identity.decode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

interface ArticleVM: Clearable {
    val state: StateFlow<State>
    val article: StateFlow<Resource<Article>>
    val paragraphs: StateFlow<Resource<List<BaseViewItem<*>>>>
    val eventFlow: Flow<Event>
    val dictPaths: StateFlow<Resource<List<String>>>

    val definitionsVM: DefinitionsVM

    fun onWordDefinitionHidden()
    fun onBackPressed()
    fun onTextClicked(sentence: NLPSentence, offset: Int): Boolean
    fun onTryAgainClicked()
    fun onPhrasalVerbSelectionChanged()
    fun onCardSetWordSelectionChanged()
    fun onPartOfSpeechSelectionChanged(partOfSpeech: WordTeacherWord.PartOfSpeech)
    fun onPhraseSelectionChanged(phraseType: ChunkType)
    fun onDictSelectionChanged(dictPath: String)

    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?

    @Parcelize
    data class State(
        var id: Long,
        var definitionsState: DefinitionsVM.State,
        var selectionState: SelectionState = SelectionState()
    ) : Parcelable

    @Parcelize
    @Serializable
    data class SelectionState(
        var partsOfSpeech: Set<WordTeacherWord.PartOfSpeech> = emptySet(),
        var phrases: Set<ChunkType> = emptySet(),
        var cardSetWords: Boolean = true,
        var phrasalVerbs: Boolean = true,
        var dicts: List<String> = emptyList()
    ) : Parcelable
}

open class ArticleVMImpl(
    override val definitionsVM: DefinitionsVM,
    private val articleRepository: ArticleRepository,
    private val cardsRepository: CardsRepository,
    private val dictRepository: DictRepository,
    initialState: ArticleVM.State,
    private val router: ArticleRouter,
    private val idGenerator: IdGenerator,
    private val settings: FlowSettings
): ViewModel(), ArticleVM {

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()
    override val article: StateFlow<Resource<Article>> = articleRepository.article
    override val state = MutableStateFlow(initialState)

    private val cardProgress = MutableStateFlow<Resource<Map<Pair<String, WordTeacherWord.PartOfSpeech>, Card>>>(
        Resource.Uninitialized())
    private val annotations = MutableStateFlow<List<List<ArticleAnnotation>>>(emptyList())

    override val paragraphs = combine(article, annotations) { a, b -> a to b }
        .map { (article, annotations) ->
            //Logger.v("build view items")
            article.copyWith(buildViewItems(article, annotations))
        }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    private val dicts: StateFlow<Resource<List<Dict>>> = dictRepository.dicts
    override val dictPaths: StateFlow<Resource<List<String>>> = dicts.map {
            it.copyWith(it.data()?.map { dict -> dict.path.name })
        }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    init {
        viewModelScope.launch(Dispatchers.Default) {
            cardsRepository.cards.map { res ->
                res.copyWith(
                    res.data()?.associateBy { it.term to it.partOfSpeech }
                )
            }.collect(cardProgress)
        }

        viewModelScope.launch(Dispatchers.Default) {
            combine(article, cardProgress, dicts, state) { a, b, c, d -> Quadruple(a, b, c, d) }
                .map { (article, cards, dicts, state) ->
                    if (article is Resource.Loaded && cards is Resource.Loaded) {
                        article.data.sentences.map {
                            resolveAnnotations(it, cards, dicts.data().orEmpty(), state.selectionState)
                        }
                    } else {
                        emptyList()
                    }
                }.collect(annotations)
        }

        viewModelScope.launch {
            state.collect {
                val selectionStateString = withContext(Dispatchers.Default) {
                    Json {
                        ignoreUnknownKeys = true
                    }.encodeToString(it.selectionState)

                }

                settings.putString("dicts", selectionStateString)
            }
        }
    }

    private fun resolveAnnotations(
        sentence: NLPSentence,
        cards: Resource.Loaded<Map<Pair<String, WordTeacherWord.PartOfSpeech>, Card>>,
        dicts: List<Dict>,
        selectionState: ArticleVM.SelectionState
    ): List<ArticleAnnotation> {
        val progressAndPartOfSpeechAnnotations = sentence.tokenSpans.indices.mapNotNull { i ->
            val span = sentence.tokenSpans[i]
            val term = sentence.lemmaOrToken(i)
            val partOfSpeech = sentence.tagEnum(i).toPartOfSpeech()

            // TODO: that won't work with phrases
            val progressAnnotation = if (selectionState.cardSetWords) {
                val card = cards.data[term to partOfSpeech] ?: cards.data[term to WordTeacherWord.PartOfSpeech.Undefined]
                card?.let { safeCard ->
                    ArticleAnnotation.LearnProgress(
                        start = span.start,
                        end = span.end,
                        learnLevel = safeCard.progress.currentLevel
                    )
                }
            } else {
                null
            }

            val partOfSpeechAnnotation = if (selectionState.partsOfSpeech.contains(partOfSpeech)) {
                ArticleAnnotation.PartOfSpeech(
                    start = span.start,
                    end = span.end,
                    partOfSpeech = partOfSpeech
                )
            } else {
                null
            }

            listOfNotNull(progressAnnotation, partOfSpeechAnnotation)
        }.flatten()

        val phrases = sentence.phrases()
        val phraseAnnotations = phrases.mapNotNull { phrase ->
            if (selectionState.phrases.contains(phrase.type)) {
                ArticleAnnotation.Phrase(
                    start = sentence.tokenSpans[phrase.start].start,
                    end = sentence.tokenSpans[phrase.end - 1].end,
                    phrase = phrase.type
                )
            } else {
                null
            }
        }

        val actualDicts = selectionState.dicts.mapNotNull { s -> dicts.firstOrNull { it.path.name == s } }
        val dictAnnotationResolver = DictAnnotationResolver()
        val dictAnnotations = dictAnnotationResolver.resolve(actualDicts, sentence, phrases)

        return progressAndPartOfSpeechAnnotations + phraseAnnotations + dictAnnotations
    }

    fun restore(newState: ArticleVM.State) {
        state.value = newState
        definitionsVM.restore(newState.definitionsState)

        viewModelScope.launch {
            articleRepository.loadArticle(newState.id)
        }
    }

    private fun buildViewItems(
        article: Resource<Article>,
        annotations: List<List<ArticleAnnotation>>
    ): List<BaseViewItem<*>> {
        return when (article) {
            is Resource.Loaded -> {
                makeParagraphs(article, annotations)
            }
            else -> emptyList()
        }
    }

    private fun makeParagraphs(
        article: Resource.Loaded<Article>,
        annotations: List<List<ArticleAnnotation>>
    ): MutableList<BaseViewItem<*>> {
        val paragraphList = mutableListOf<BaseViewItem<*>>()

        article.data.style.paragraphs.onEach { paragraph ->
            paragraphList.add(
                ParagraphViewItem(
                    idGenerator.nextId(),
                    sentences = article.data.sentences.split(paragraph),
                    annotations = if (annotations.isNotEmpty()) {
                        annotations.split(paragraph)
                    } else {
                        emptyList()
                    }
                )
            )
        }

        return paragraphList
    }

    override fun onTextClicked(sentence: NLPSentence, offset: Int): Boolean {
        val slice = sentence.sliceFromTextIndex(offset)
        if (slice != null) {
            val sentenceIndex = article.value.data()?.sentences?.indexOf(sentence) ?: return false
            val sentenceAnnotations = annotations.value[sentenceIndex].filterIsInstance<ArticleAnnotation.DictWord>()
            val annotations = sentenceAnnotations.filter {
                slice.tokenSpan.start >= it.start && slice.tokenSpan.end <= it.end
            }
            val firstAnnotation = annotations.firstOrNull()
            val resultWord = firstAnnotation?.entry?.word ?: slice.tokenString
            val resultPartOfSpeech = firstAnnotation?.entry?.partOfSpeech ?: slice.partOfSpeech()
            val resultPartOfSpeechList = if (resultPartOfSpeech == WordTeacherWord.PartOfSpeech.PhrasalVerb) {
                listOf(resultPartOfSpeech, WordTeacherWord.PartOfSpeech.Verb)
            } else {
                listOf(resultPartOfSpeech)
            }

            definitionsVM.onWordSubmitted(
                resultWord,
                resultPartOfSpeechList + WordTeacherWord.PartOfSpeech.Undefined, // undefined to show dict result,
                DefinitionsContext(
                    wordContexts = mapOf(
                        resultPartOfSpeech to DefinitionsWordContext(
                            examples = listOf(sentence.text)
                        )
                    )
                )
            )
        }

        return slice != null
    }

    override fun onPhrasalVerbSelectionChanged() =
        state.update {
            it.copy(
                selectionState = it.selectionState.copy(
                    phrasalVerbs = !it.selectionState.phrasalVerbs
                )
            )
        }

    override fun onCardSetWordSelectionChanged() =
        state.update {
            it.copy(
                selectionState = it.selectionState.copy(
                    cardSetWords = !it.selectionState.cardSetWords
                )
            )
        }

    override fun onPartOfSpeechSelectionChanged(partOfSpeech: WordTeacherWord.PartOfSpeech) {
        state.update {
            val partsOfSpeech = it.selectionState.partsOfSpeech
            val needRemove = partsOfSpeech.contains(partOfSpeech)
            it.copy(
                selectionState = it.selectionState.copy(
                    partsOfSpeech = if (needRemove) {
                        partsOfSpeech.minus(partOfSpeech)
                    } else {
                        partsOfSpeech.plus(partOfSpeech)
                    }
                )
            )
        }
    }

    override fun onPhraseSelectionChanged(phraseType: ChunkType) {
        state.update {
            val phrases = it.selectionState.phrases
            val needRemove = phrases.contains(phraseType)
            it.copy(
                selectionState = it.selectionState.copy(
                    phrases = if (needRemove) {
                        phrases.minus(phraseType)
                    } else {
                        phrases.plus(phraseType)
                    }
                )
            )
        }
    }

    override fun onDictSelectionChanged(dictPath: String) {
        state.update {
            val dicts = it.selectionState.dicts
            val needRemove = dicts.contains(dictPath)
            it.copy(
                selectionState = it.selectionState.copy(
                    dicts = if (needRemove) {
                        dicts.minus(dictPath)
                    } else {
                        dicts.plus(dictPath)
                    }
                )
            )
        }
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.article_error)
    }

    override fun onTryAgainClicked() {
        // TODO: do sth with articlesRepository
    }

    override fun onWordDefinitionHidden() {
        definitionsVM.onWordSubmitted(null)
    }

    override fun onBackPressed() {
        router.closeArticle()
    }

    override fun onCleared() {
        super.onCleared()
        articleRepository.cancel()
        cardsRepository.cancel()
        eventChannel.cancel()
    }
}

sealed class ArticleAnnotation(
    val type: ArticleAnnotationType,
    val start: Int,
    val end: Int
) {
    class LearnProgress(start: Int, end: Int, val learnLevel: Int): ArticleAnnotation(ArticleAnnotationType.LEARN_PROGRESS, start, end)
    class PartOfSpeech(start: Int, end: Int, val partOfSpeech: WordTeacherWord.PartOfSpeech): ArticleAnnotation(ArticleAnnotationType.PART_OF_SPEECH, start, end)
    class Phrase(start: Int, end: Int, val phrase: ChunkType): ArticleAnnotation(ArticleAnnotationType.PHRASE, start, end)
    class DictWord(start: Int, end: Int, val entry: Dict.Index.Entry, val dict: Dict): ArticleAnnotation(ArticleAnnotationType.DICT, start, end)
}

enum class ArticleAnnotationType {
    LEARN_PROGRESS,
    PART_OF_SPEECH,
    PHRASE,
    DICT
}
