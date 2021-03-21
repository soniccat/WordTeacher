package com.aglushkov.wordteacher.shared.features.article.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayMode
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.model.ShortArticle
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

interface ArticleVM {
    val state: State
    val paragraphs: MutableStateFlow<Resource<List<BaseViewItem<*>>>>

    @Parcelize
    class State(
        var id: Long,
        var state: State
    ) : Parcelable
}


class ArticleVMImpl(
    private val definitionsVM: DefinitionsVM,
    private val articleRepository: ArticleRepository,
    override val state: ArticleVM.State,
    private val router: ArticleRouter,
    private val idGenerator: IdGenerator,
): ViewModel(), ArticleVM {

    private val mutableEventFlow = MutableSharedFlow<Event>(
        replay = Int.MAX_VALUE,
        extraBufferCapacity = Int.MAX_VALUE
    )
    val eventFlow: SharedFlow<Event> = mutableEventFlow
    private val article: StateFlow<Resource<Article>> = articleRepository.article
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

    private fun saveArticle() = viewModelScope.launch {
        // TODO:
//        try {
//            articlesRepository.createArticle(article)
//            mutableEventFlow.emit(CompletionEvent(CompletionResult.COMPLETED))
//        } catch (e: CancellationException) {
//            throw e
//        } catch (e: Exception) {
//            val errorText = e.message?.let {
//                StringDesc.Raw(it)
//            } ?: StringDesc.Resource(MR.strings.error_default)
//
//            mutableEventFlow.emit(ErrorEvent(errorText))
//        }
    }

    private fun buildViewItems(article: Resource<Article>): List<BaseViewItem<*>> {
        return when (article) {
            is Resource.Loaded -> {
                listOf(
                    ParagraphViewItem(
                        idGenerator.nextId(),
                        article.data.sentences
                    )
                )
            }
            else -> emptyList()
        }
    }

    fun onWordClicked(word: String) = viewModelScope.launch {
//        mutableEventFlow.emit(ShowDefinitionEvent(word))
        definitionsVM.onWordSubmitted(word)
    }

    fun onBackPressed() {
        router.closeArticle()
    }
}

data class ShowDefinitionEvent(val word: String): Event