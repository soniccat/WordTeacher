package com.aglushkov.wordteacher.shared.features.article.vm

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsContext
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsWordContext
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.Quadruple
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
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
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ArticleVM: Clearable {
    val state: StateFlow<State>
    val article: StateFlow<Resource<Article>>
    val paragraphs: StateFlow<Resource<List<BaseViewItem<*>>>>
    val dictPaths: StateFlow<Resource<List<String>>>
    val definitionsVM: DefinitionsVM

    var router: ArticleRouter?

    fun onWordDefinitionHidden()
    fun onBackPressed()
    fun onTextClicked(sentence: NLPSentence, offset: Int): Boolean
    fun onAnnotationChooserClicked(index: Int?)
    fun onTryAgainClicked()
    fun onCardSetWordSelectionChanged()
    fun onPartOfSpeechSelectionChanged(partOfSpeech: WordTeacherWord.PartOfSpeech)
    fun onPhraseSelectionChanged(phraseType: ChunkType)
    fun onDictSelectionChanged(dictPath: String)
    fun onFirstItemIndexChanged(index: Int)

    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?

    // TODO: simplify
    class StateController(
        id: Long,
        private val settings: FlowSettings,
    ) {
        private val SELECTION_STATE_KEY = "articleSelectionState"
        private val FIRSTITEMINDEX_STATE_KEY = "articleFirstItemState"
        private val jsonCoder = Json {
            ignoreUnknownKeys = true
        }
        private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        private var inMemoryState = runBlocking {
            InMemoryState(
                id = id,
                selectionState =
                    settings.getString(SELECTION_STATE_KEY, "{}").let {
                        try {
                            jsonCoder.decodeFromString(it) as SelectionState
                        } catch (e: Exception) {
                            SelectionState()
                        }
                    }
                ,
                lastFirstVisibleItemMap = settings.getString(FIRSTITEMINDEX_STATE_KEY, "{}").let {
                    try {
                        (jsonCoder.decodeFromString(it) as Map<Long, Int>)
                    } catch (e: Exception) {
                        emptyMap()
                    }
                }
            )
        }

        private val mutableFlow = MutableStateFlow(inMemoryState)
        val stateFlow: StateFlow<State> = mutableFlow.map { it.toState() }
            .stateIn(mainScope, SharingStarted.Eagerly, inMemoryState.toState())

        var selectionState: SelectionState
            get() = inMemoryState.selectionState
            set(value) {
                inMemoryState = inMemoryState.copy(selectionState = value)
                mutableFlow.update { inMemoryState }
                scope.launch {
                    val stringValue = jsonCoder.encodeToString(value)
                    settings.putString(SELECTION_STATE_KEY, stringValue)
                }
            }

        var lastFirstVisibleItem: Int
            get() = inMemoryState.lastFirstVisibleItem
            set(value) {
                if (value != inMemoryState.lastFirstVisibleItem) {
                    inMemoryState = inMemoryState.updateWithLastFirstVisibleItem(value)
                    mutableFlow.update { inMemoryState }

                    val mapToStore = inMemoryState.lastFirstVisibleItemMap
                    scope.launch {
                        val stringValue = jsonCoder.encodeToString(mapToStore)
                        settings.putString(FIRSTITEMINDEX_STATE_KEY, stringValue)
                    }
                }
            }

        fun updateSelectionState(block: (SelectionState) -> SelectionState) {
            this.selectionState = block(this.selectionState)
        }

        fun updateAnnotationChooserState(block: (AnnotationChooserState?) -> AnnotationChooserState?) {
            inMemoryState = inMemoryState.copy(annotationChooserState = block(inMemoryState.annotationChooserState))
            mutableFlow.update { inMemoryState }
        }
    }

    private data class InMemoryState(
        val id: Long,
        val selectionState: SelectionState = SelectionState(),
        val lastFirstVisibleItemMap: Map<Long, Int> = emptyMap(), // keep the whole map not to reload it repeatedly
        val annotationChooserState: AnnotationChooserState? = null,
    ) {
        val lastFirstVisibleItem: Int
            get() = lastFirstVisibleItemMap[id] ?: 0

        fun toState() = State(id = id, selectionState = selectionState, lastFirstVisibleItem = lastFirstVisibleItem)
        fun updateWithLastFirstVisibleItem(index: Int) =
            copy(
                lastFirstVisibleItemMap = lastFirstVisibleItemMap + (id to index)
            )
    }

    @Parcelize
    data class State(
        val id: Long,
        val selectionState: SelectionState = SelectionState(),
        val lastFirstVisibleItem: Int = 0,
    ) : Parcelable

    @Parcelize
    @Serializable
    data class SelectionState(
        val partsOfSpeech: Set<WordTeacherWord.PartOfSpeech> = emptySet(),
        val phrases: Set<ChunkType> = emptySet(),
        val cardSetWords: Boolean = true,
        val dicts: List<String> = emptyList()
    ) : Parcelable

    data class AnnotationChooserState(
        val sentenceIndex: Int,
        val sentenceOffset: Int,
        val annotations: List<ArticleAnnotation.DictWord>
    )
}

open class ArticleVMImpl(
    override val definitionsVM: DefinitionsVM,
    private val articleRepository: ArticleRepository,
    private val cardsRepository: CardsRepository,
    private val dictRepository: DictRepository,
    articleId: Long,
    private val idGenerator: IdGenerator,
    settings: FlowSettings
): ViewModel(), ArticleVM {
    override var router: ArticleRouter? = null
    override val article: StateFlow<Resource<Article>> = articleRepository.article

    private val stateController = ArticleVM.StateController(
        id = articleId,
        settings = settings
    )
    override val state = stateController.stateFlow

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
            combine(article, cardProgress, dicts, state.map { it.selectionState }.distinctUntilChanged()) { a, b, c, d -> Quadruple(a, b, c, d) }
                .map { (article, cards, dicts, selectionState) ->
                    if (article is Resource.Loaded && cards is Resource.Loaded) {
                        article.data.sentences.map {
                            resolveAnnotations(it, cards, dicts.data().orEmpty(), selectionState)
                        }
                    } else {
                        emptyList()
                    }
                }.collect(annotations)
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
        val slice = sentence.sliceFromTextIndex(offset, true)
        if (slice != null && slice.tokenSpan.length > 1) {
            val sentenceIndex = article.value.data()?.sentences?.indexOf(sentence) ?: return false
            val sentenceAnnotations = annotations.value[sentenceIndex].filterIsInstance<ArticleAnnotation.DictWord>()
            val annotations = sentenceAnnotations.filter {
                slice.tokenSpan.start >= it.start && slice.tokenSpan.end <= it.end
            }

            if (annotations.isNotEmpty()) {
                stateController.updateAnnotationChooserState { ArticleVM.AnnotationChooserState(sentenceIndex, offset, annotations) }
                return false
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

    override fun onAnnotationChooserClicked(index: Int?) {
        stateController.updateAnnotationChooserState { null }
    }

    override fun onCardSetWordSelectionChanged() =
        stateController.updateSelectionState {
            it.copy(cardSetWords = !it.cardSetWords)
        }

    override fun onPartOfSpeechSelectionChanged(partOfSpeech: WordTeacherWord.PartOfSpeech) {
        stateController.updateSelectionState {
            val needRemove = it.partsOfSpeech.contains(partOfSpeech)
            it.copy(
                partsOfSpeech = if (needRemove) {
                    it.partsOfSpeech.minus(partOfSpeech)
                } else {
                    it.partsOfSpeech.plus(partOfSpeech)
                }
            )
        }
    }

    override fun onPhraseSelectionChanged(phraseType: ChunkType) {
        stateController.updateSelectionState {
            val needRemove = it.phrases.contains(phraseType)
            it.copy(
                phrases = if (needRemove) {
                    it.phrases.minus(phraseType)
                } else {
                    it.phrases.plus(phraseType)
                }
            )
        }
    }

    override fun onDictSelectionChanged(dictPath: String) {
        stateController.updateSelectionState {
            val needRemove = it.dicts.contains(dictPath)
            it.copy(
                dicts = if (needRemove) {
                    it.dicts.minus(dictPath)
                } else {
                    it.dicts.plus(dictPath)
                }
            )
        }
    }

    override fun onFirstItemIndexChanged(index: Int) {
        stateController.lastFirstVisibleItem = index
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
        router?.closeArticle()
    }

    override fun onCleared() {
        super.onCleared()
        articleRepository.cancel()
        cardsRepository.cancel()
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
