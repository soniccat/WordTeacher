package com.aglushkov.wordteacher.shared.features.article.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
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
    class State(
        var id: Long,
        var definitionsState: DefinitionsVM.State
    ) : Parcelable
}


class ArticleVMImpl(
    private val definitionsVM: DefinitionsVM,
    private val articleRepository: ArticleRepository,
    override val state: ArticleVM.State,
    private val router: ArticleRouter,
    private val idGenerator: IdGenerator,
): ViewModel(), ArticleVM {

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()
    override val article: StateFlow<Resource<Article>> = articleRepository.article
    override val paragraphs = MutableStateFlow<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())

    init {
        viewModelScope.launch {
            articleRepository.loadArticle(state.id)
        }

        viewModelScope.launch {
            article.map {
                Logger.v("build view items")
                it.copyWith(buildViewItems(it))
            }.forward(paragraphs)
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
                definitionsVM.onWordSubmitted(it.tokenString)
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