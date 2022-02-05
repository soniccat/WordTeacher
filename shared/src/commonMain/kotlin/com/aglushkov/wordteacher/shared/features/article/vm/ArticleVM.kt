package com.aglushkov.wordteacher.shared.features.article.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsContext
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsWordContext
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.split
import com.aglushkov.wordteacher.shared.model.nlp.toPartOfSpeech
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardsRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

interface ArticleVM: Clearable {
    val state: StateFlow<State>
    val article: StateFlow<Resource<Article>>
    val paragraphs: StateFlow<Resource<List<BaseViewItem<*>>>>
    val eventFlow: Flow<Event>

    val definitionsVM: DefinitionsVM

    fun onWordDefinitionHidden()
    fun onBackPressed()
    fun onTextClicked(sentence: NLPSentence, offset: Int): Boolean
    fun onTryAgainClicked()
    fun onPhrasalVerbSelectionChanged()
    fun onCardSetWordSelectionChanged()
    fun onPartOfSpeechSelectionChanged(partOfSpeech: WordTeacherWord.PartOfSpeech)

    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?

    @Parcelize
    data class State(
        var id: Long,
        var definitionsState: DefinitionsVM.State,
        var selectionState: SelectionState = SelectionState()
    ) : Parcelable

    @Parcelize
    data class SelectionState(
        var partsOfSpeech: Set<WordTeacherWord.PartOfSpeech> = emptySet(),
        var cardSetWords: Boolean = true,
        var phrasalVerbs: Boolean = true
    ) : Parcelable
}

open class ArticleVMImpl(
    override val definitionsVM: DefinitionsVM,
    private val articleRepository: ArticleRepository,
    private val cardsRepository: CardsRepository,
    private var initialState: ArticleVM.State,
    private val router: ArticleRouter,
    private val idGenerator: IdGenerator,
): ViewModel(), ArticleVM {

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()
    override val article: StateFlow<Resource<Article>> = articleRepository.article
    override val state = MutableStateFlow(initialState)

    private val cardProgress = MutableStateFlow<Resource<Map<Pair<String, WordTeacherWord.PartOfSpeech>, Card>>>(
        Resource.Uninitialized())
    private val annotations = MutableStateFlow<List<List<ArticleAnnotation>>>(emptyList())

    override val paragraphs = combine(article, annotations) { a, b -> a to b}
        .map { (article, annotations) ->
            Logger.v("build view items")
            article.copyWith(buildViewItems(article, annotations))
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
            combine(article, cardProgress) { a, b -> a to b }
                .map { (article, cards) ->
                    if (article is Resource.Loaded && cards is Resource.Loaded) {
                        article.data.sentences.map {
                            resolveAnnotations(it, cards)
                        }
                    } else {
                        emptyList()
                    }
                }.collect(annotations)
        }
    }

    private fun resolveAnnotations(
        sentence: NLPSentence,
        cards: Resource.Loaded<Map<Pair<String, WordTeacherWord.PartOfSpeech>, Card>>
    ) = sentence.tokenSpans.indices.mapNotNull { i ->
        val span = sentence.tokenSpans[i]
        val term = sentence.lemmaOrToken(i)
        val partOfSpeech = sentence.tagEnum(i).toPartOfSpeech()

        val progressAnnotation = cards.data[term to partOfSpeech]?.let { card ->
            ArticleAnnotation.LearnProgress(
                start = span.start,
                end = span.end,
                learnLevel = card.progress.currentLevel
            )
        }

        val partOfSpeechAnnotation = ArticleAnnotation.PartOfSpeech(
            start = span.start,
            end = span.end,
            partOfSpeech = partOfSpeech
        )

        listOfNotNull(progressAnnotation, partOfSpeechAnnotation)
    }.flatten()

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
            definitionsVM.onWordSubmitted(
                slice.tokenString,
                listOf(slice.partOfSpeech()),
                DefinitionsContext(
                    wordContexts = mapOf(
                        slice.partOfSpeech() to DefinitionsWordContext(
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
}

enum class ArticleAnnotationType {
    LEARN_PROGRESS,
    PART_OF_SPEECH
}