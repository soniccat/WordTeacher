package com.aglushkov.wordteacher.shared.features.article.vm

import androidx.datastore.preferences.core.Preferences
import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.features.dashboard.vm.HintViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsContext
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsWordContext
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.Quadruple
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.onData
import com.aglushkov.wordteacher.shared.general.settings.HintType
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.general.settings.isHintClosed
import com.aglushkov.wordteacher.shared.general.settings.serializable
import com.aglushkov.wordteacher.shared.general.settings.setHintClosed
import com.aglushkov.wordteacher.shared.general.settings.setSerializable
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.model.ArticleStyle
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.nlp.ChunkType
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceSlice
import com.aglushkov.wordteacher.shared.model.nlp.split
import com.aglushkov.wordteacher.shared.model.nlp.toPartOfSpeech
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardsRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.collections.orEmpty

interface ArticleVM: Clearable {
    val state: StateFlow<InMemoryState>
    val article: StateFlow<Resource<Article>>
    val paragraphs: StateFlow<Resource<List<BaseViewItem<*>>>>
    val dictPaths: StateFlow<Resource<List<String>>>
    val definitionsVM: DefinitionsVM
    val lastFirstVisibleItem: Int

    var router: ArticleRouter?

    fun onWordDefinitionHidden()
    fun onBackPressed()
    fun onTextClicked(sentence: NLPSentence, offset: Int)
    fun onTextLongPressed(sentence: NLPSentence, offset: Int)
    fun onAnnotationChooserClicked(index: Int?)
    fun onTryAgainClicked()
    fun onCardSetWordSelectionChanged()
    fun onPartOfSpeechSelectionChanged(partOfSpeech: WordTeacherWord.PartOfSpeech)
    fun onPhraseSelectionChanged(phraseType: ChunkType)
    fun onDictSelectionChanged(dictPath: String)
    fun onFilterDictSingleWordEntriesChanged()
    fun onFirstItemIndexChanged(index: Int)
    fun onWordDefinitionShown()
    fun onMarkAsReadUnreadClicked()
    fun onHintClicked(hintType: HintType)

    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?

    // TODO: simplify
    class StateController(
        restoredState: State,
        private val settings: SettingStore,
    ) {
        private val SELECTION_STATE_KEY = "articleSelectionState"
        private var inMemoryState =
            InMemoryState(
                id = restoredState.id,
                isRead = false,
                // TODO: move that into ArticlesRepository
                selectionState =
                    settings.serializable(SELECTION_STATE_KEY) ?: SelectionState(),
            )

        val articleId = restoredState.id
        private val mutableFlow = MutableStateFlow(inMemoryState)
        val stateFlow: StateFlow<InMemoryState> = mutableFlow

        private var selectionState: SelectionState
            get() = inMemoryState.selectionState
            set(value) {
                inMemoryState = inMemoryState.update { copy(selectionState = value) }
                mutableFlow.update { inMemoryState }
                settings.setSerializable(SELECTION_STATE_KEY, value)
            }

        fun updateSelectionState(block: (SelectionState) -> SelectionState) {
            this.selectionState = block(this.selectionState)
        }

        fun updateAnnotationChooserState(block: (AnnotationChooserState?) -> AnnotationChooserState?) {
            inMemoryState = inMemoryState.update { copy(annotationChooserState = block(inMemoryState.annotationChooserState)) }
            mutableFlow.update { inMemoryState }
        }

        fun updateNeedShowWordDefinition(needShowWordDefinition: Boolean) {
            inMemoryState = inMemoryState.update { copy(needShowWordDefinition = needShowWordDefinition) }
            mutableFlow.update { inMemoryState }
        }

        fun updateIsReadState(isRead: Boolean) {
            inMemoryState = inMemoryState.update { copy(isRead = isRead) }
            mutableFlow.update { inMemoryState }
        }
    }

    data class InMemoryState(
        val version: Int = 0,
        val id: Long,
        val isRead: Boolean,
        val selectionState: SelectionState = SelectionState(),
        val needShowWordDefinition: Boolean = false,
        val annotationChooserState: AnnotationChooserState? = null,
    ) {
        fun update(block: InMemoryState.()->InMemoryState) = run { block(this).copy(version = version + 1) }
        fun toState() = State(id = id)
    }

    @Serializable
    data class State(
        val id: Long,
    )

    @Serializable
    data class SelectionState(
        val partsOfSpeech: Set<WordTeacherWord.PartOfSpeech> = emptySet(),
        val phrases: Set<ChunkType> = emptySet(),
        val cardSetWords: Boolean = true,
        val dicts: List<String> = listOf("words.wordlist"),
        val filterDictSingleWordEntries: Boolean = true,
    )

    data class AnnotationChooserState(
        val sentenceIndex: Int,
        val sentenceOffset: Int,
        val annotations: List<ArticleAnnotation.DictWord>,
        val sentence: NLPSentence,
        val sentenceSlice: NLPSentenceSlice,
    )
}

open class ArticleVMImpl(
    restoredState: ArticleVM.State,
    override val definitionsVM: DefinitionsVM,
    private val articleRepository: ArticleRepository,
    private val articlesRepository: ArticlesRepository,
    private val cardsRepository: CardsRepository,
    private val dictRepository: DictRepository,
    private val idGenerator: IdGenerator,
    private val settingStore: SettingStore,
    private val analytics: Analytics,
): ViewModel(), ArticleVM {
    override var router: ArticleRouter? = null
    override val article: StateFlow<Resource<Article>> = articleRepository.article

    private val stateController = ArticleVM.StateController(
        restoredState = restoredState,
        settings = settingStore,
    )
    override val state = stateController.stateFlow

    private val cardProgress = MutableStateFlow<Resource<Map<Pair<String, WordTeacherWord.PartOfSpeech>, Card>>>(
        Resource.Uninitialized())
    private val annotations = MutableStateFlow<List<List<ArticleAnnotation>>>(emptyList())

    override val paragraphs = combine(article, annotations, settingStore.prefs) { article, annotations, prefs ->
            //Logger.v("build view items")
            article.copyWith(buildViewItems(article, annotations, prefs))
        }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    private val dicts: StateFlow<Resource<List<Dict>>> = dictRepository.dicts
    override val dictPaths: StateFlow<Resource<List<String>>> = dicts.map {
            it.copyWith(it.data()?.map { dict -> dict.path.name })
        }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    override val lastFirstVisibleItem: Int
        get() = articlesRepository.lastFirstVisibleItemMap.value.data()?.get(stateController.articleId) ?: 0

    init {
        articleRepository.loadArticle(state.value.id)

        // handle markIsRead changes
        viewModelScope.launch {
            articleRepository.article.count {
                it.onData {
                    stateController.updateIsReadState(it.isRead)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
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
        val dictAnnotations = dictAnnotationResolver.resolve(actualDicts, sentence, phrases).run {
            if (selectionState.filterDictSingleWordEntries) {
                // TODO: move that logic in dictAnnotationResolver instead of filtering here
                filter {
                    it.entry.word.contains(' ') // contains word delimiter
                }
            } else {
                this
            }
        }

        return progressAndPartOfSpeechAnnotations + phraseAnnotations + dictAnnotations
    }

    private fun buildViewItems(
        article: Resource<Article>,
        annotations: List<List<ArticleAnnotation>>,
        prefs: Preferences,
    ): List<BaseViewItem<*>> {
        return when (article) {
            is Resource.Loaded -> {
                val resultList = mutableListOf<BaseViewItem<*>>()
                if (!prefs.isHintClosed(HintType.Article)) {
                    resultList.add(HintViewItem(HintType.Article))
                }

                resultList.addAll(makeParagraphs(article, annotations))
                generateIds(resultList)
                resultList
            }
            else -> emptyList()
        }
    }

    private fun makeParagraphs(
        article: Resource.Loaded<Article>,
        annotations: List<List<ArticleAnnotation>>
    ): MutableList<BaseViewItem<*>> {
        val paragraphList = mutableListOf<BaseViewItem<*>>()
        var isLastHeader = false

        article.data.style.paragraphs.onEach { paragraph ->
            val sentences = article.data.sentences.split(paragraph)
            val styles = ArticleStyle(
                headers = article.data.style.headers
                    .filter { style ->
                        paragraph.start <= style.sentenceIndex && paragraph.end >= style.sentenceIndex
                    }.map {
                        it.copy(sentenceIndex = it.sentenceIndex - paragraph.start)
                    }
            )
            val isHeader = sentences.size == 1 &&
                    styles.headers.size == 1 &&
                    styles.headers[0].start == 0 &&
                    styles.headers[0].end == sentences[0].text.length

            paragraphList.add(
                ParagraphViewItem(
                    paragraphId = 0L,
                    sentences = sentences,
                    annotations = if (annotations.isNotEmpty()) {
                        annotations.split(paragraph)
                    } else {
                        emptyList()
                    },
                    styles = styles,
                    isHeader = isHeader,
                    isBelowHeader = isLastHeader,
                )
            )

            isLastHeader = isHeader
        }

        return paragraphList
    }

    private fun generateIds(items: MutableList<BaseViewItem<*>>) {
        generateViewItemIds(items, paragraphs.value.data().orEmpty(), idGenerator)
    }

    override fun onTextClicked(sentence: NLPSentence, offset: Int) {
        analytics.send(AnalyticEvent.createActionEvent("Article.textClicked"))

        val slice = sentence.sliceFromTextIndex(offset, true)
        if (slice != null && slice.tokenSpan.length > 1 && annotations.value.isNotEmpty()) {
            val sentenceIndex = article.value.data()?.sentences?.indexOf(sentence) ?: return
            val sentenceAnnotations = annotations.value[sentenceIndex].filterIsInstance<ArticleAnnotation.DictWord>()
            val annotations = sentenceAnnotations.filter {
                slice.tokenSpan.start >= it.start && slice.tokenSpan.end <= it.end
            }.distinctBy { it.entry.word }

            if (annotations.size > 1) {
                stateController.updateAnnotationChooserState {
                    ArticleVM.AnnotationChooserState(sentenceIndex, offset, annotations, sentence, slice)
                }
                return
            }

            val firstAnnotation = annotations.firstOrNull()
            showDefinition(firstAnnotation, sentence, slice)
        }
    }

    override fun onTextLongPressed(sentence: NLPSentence, offset: Int) {
        analytics.send(AnalyticEvent.createActionEvent("Article.textLongPressed"))

        // get the direct word below
        val slice = sentence.sliceFromTextIndex(offset, true)
        if (slice != null && slice.tokenSpan.length > 1 && annotations.value.isNotEmpty()) {
            showDefinition(null, sentence, slice)
        }
    }

    private fun showDefinition(
        firstAnnotation: ArticleAnnotation.DictWord?,
        sentence: NLPSentence,
        slice: NLPSentenceSlice,
    ) {
        val resultWord = firstAnnotation?.entry?.word ?: slice.tokenString
        val resultPartOfSpeech = firstAnnotation?.entry?.partOfSpeech ?: slice.partOfSpeech()
        val resultPartOfSpeechList = when(resultPartOfSpeech) {
            // undefined to show dict results
            WordTeacherWord.PartOfSpeech.Verb -> listOf(resultPartOfSpeech, WordTeacherWord.PartOfSpeech.PhrasalVerb, WordTeacherWord.PartOfSpeech.Undefined)
            WordTeacherWord.PartOfSpeech.PhrasalVerb -> listOf(resultPartOfSpeech, WordTeacherWord.PartOfSpeech.Verb, WordTeacherWord.PartOfSpeech.Undefined)
            WordTeacherWord.PartOfSpeech.Undefined -> listOf()
            else -> listOf(resultPartOfSpeech, WordTeacherWord.PartOfSpeech.Undefined)
        }

        definitionsVM.onWordSubmitted(
            resultWord,
            resultPartOfSpeechList,
            DefinitionsContext(
                wordContexts = mapOf(
                    resultPartOfSpeech to DefinitionsWordContext(
                        examples = listOf(sentence.text)
                    )
                )
            )
        )

        stateController.updateNeedShowWordDefinition(true)
    }

    override fun onAnnotationChooserClicked(index: Int?) {
        analytics.send(AnalyticEvent.createActionEvent("Article.annotationChooserClicked"))

        val chooserState = state.value.annotationChooserState ?: return
        val annotation = chooserState.annotations.getOrNull(index ?: -1)
        stateController.updateAnnotationChooserState { null }

        annotation?.let { a ->
            showDefinition(a, chooserState.sentence, chooserState.sentenceSlice)
        }
    }

    override fun onCardSetWordSelectionChanged() {
        analytics.send(AnalyticEvent.createActionEvent("Article.cardSetWordSelectionChanged"))
        stateController.updateSelectionState {
            it.copy(cardSetWords = !it.cardSetWords)
        }
    }

    override fun onPartOfSpeechSelectionChanged(partOfSpeech: WordTeacherWord.PartOfSpeech) {
        analytics.send(AnalyticEvent.createActionEvent("Article.partOfSpeechSelectionChanged", mapOf("partOfSpeech" to partOfSpeech.name)))
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
        analytics.send(AnalyticEvent.createActionEvent("Article.phraseSelectionChanged", mapOf("phraseType" to phraseType.name)))
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
        analytics.send(AnalyticEvent.createActionEvent("Article.dictSelectionChanged"))
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

    override fun onFilterDictSingleWordEntriesChanged() {
        analytics.send(AnalyticEvent.createActionEvent("Article.filterDictSingleWordEntriesChanged"))
        stateController.updateSelectionState {
            it.copy(filterDictSingleWordEntries = !it.filterDictSingleWordEntries)
        }
    }

    override fun onFirstItemIndexChanged(index: Int) {
        articlesRepository.updateLastFirstVisibleItem(stateController.articleId, index)
    }

    override fun onWordDefinitionShown() {
        stateController.updateNeedShowWordDefinition(false)
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.article_error)
    }

    override fun onTryAgainClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Article.onTryAgainClicked"))
        articleRepository.loadArticle(state.value.id)
    }

    override fun onWordDefinitionHidden() {
        definitionsVM.onWordSubmitted(null)
    }

    override fun onMarkAsReadUnreadClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Article.onMarkAsReadUnreadClicked"))
        val newState = !stateController.stateFlow.value.isRead
        stateController.updateIsReadState(newState)
        articleRepository.markAsRead(newState)
    }

    override fun onHintClicked(hintType: HintType) {
        analytics.send(AnalyticEvent.createActionEvent("Hint_" + hintType.name))
        settingStore.setHintClosed(hintType)
        articlesRepository.offsetLastFirstVisibleItem(-1)
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
