package com.aglushkov.wordteacher.shared.features.article.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.min

interface ArticleVM {
    val state: State
    val article: StateFlow<Resource<Article>>
    val paragraphs: StateFlow<Resource<List<BaseViewItem<*>>>>
    val eventFlow: Flow<Event>

    fun onWordDefinitionHidden()
    fun onBackPressed()
    fun onTextClicked(index: Int, sentence: NLPSentence)

    @Parcelize
    data class State(
        var id: Long,
        var definitionsState: DefinitionsVM.State
    ) : Parcelable
}


open class ArticleVMImpl(
    private val definitionsVM: DefinitionsVM,
    private val articleRepository: ArticleRepository,
    override var state: ArticleVM.State,
    private val router: ArticleRouter,
    private val idGenerator: IdGenerator,
): ViewModel(), ArticleVM {

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()
    override val article: StateFlow<Resource<Article>> = articleRepository.article
    override val paragraphs = article.map {
        Logger.v("build view items")
        it.copyWith(buildViewItems(it))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

//    init {
//        viewModelScope.launch {
//            articleRepository.loadArticle(state.id)
//        }
//    }

    fun restore(newState: ArticleVM.State) {
        state = newState
        viewModelScope.launch {
            articleRepository.loadArticle(state.id)
        }
    }

    private fun buildViewItems(article: Resource<Article>): List<BaseViewItem<*>> {
        return when (article) {
            is Resource.Loaded -> {
                makeParagraphs(article)
            }
            else -> emptyList()
        }
    }

    private fun makeParagraphs(article: Resource.Loaded<Article>): MutableList<BaseViewItem<*>> {
        // TODO: write proper paragraph separation
        val paragraphSize = 5
        var sentenceIndex = 0
        val paragraphList = mutableListOf<BaseViewItem<*>>()

        while (sentenceIndex < article.data.sentences.size) {
            val nextSentenceIndex = min(article.data.sentences.size, sentenceIndex + paragraphSize)
            paragraphList.add(
                ParagraphViewItem(
                    idGenerator.nextId(),
                    article.data.sentences.subList(sentenceIndex, nextSentenceIndex)
                )
            )
            sentenceIndex = nextSentenceIndex
        }
        return paragraphList
    }

    override fun onTextClicked(index: Int, sentence: NLPSentence) {
        viewModelScope.launch {
            sentence.sliceFromTextIndex(index)?.let {
                definitionsVM.onWordSubmitted(
                    it.tokenString,
                    listOf(it.partOfSpeech())
                )
            }
        }
    }

    override fun onWordDefinitionHidden() {
        definitionsVM.onWordSubmitted(null)
    }

    override fun onBackPressed() {
        router.closeArticle()
    }

    override fun onCleared() {
        super.onCleared()
        eventChannel.cancel()
    }
}