package com.aglushkov.wordteacher.shared.features.article.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsContext
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsWordContext
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.split
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.min

interface ArticleVM {
    val state: State
    val article: StateFlow<Resource<Article>>
    val paragraphs: StateFlow<Resource<List<BaseViewItem<*>>>>
    val eventFlow: Flow<Event>

    val definitionsVM: DefinitionsVM

    fun onWordDefinitionHidden()
    fun onBackPressed()
    fun onTextClicked(sentence: NLPSentence, offset: Int): Boolean
    fun onTryAgainClicked()

    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?

    @Parcelize
    data class State(
        var id: Long,
        var definitionsState: DefinitionsVM.State
    ) : Parcelable
}


open class ArticleVMImpl(
    override val definitionsVM: DefinitionsVM,
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
        definitionsVM.restore(state.definitionsState)

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
        val paragraphList = mutableListOf<BaseViewItem<*>>()

        article.data.style.paragraphs.onEach { paragraph ->
            paragraphList.add(
                ParagraphViewItem(
                    idGenerator.nextId(),
                    article.data.sentences.split(paragraph)
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
        eventChannel.cancel()
    }
}