package com.aglushkov.wordteacher.shared.features.article.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ArticleVM(
    private val articleRepository: ArticleRepository,
    val state: State,
    private val router: ArticleRouter
): ViewModel() {

    private val mutableEventFlow = MutableSharedFlow<Event>(
        replay = Int.MAX_VALUE,
        extraBufferCapacity = Int.MAX_VALUE
    )
    val eventFlow: SharedFlow<Event> = mutableEventFlow
    val article: StateFlow<Resource<Article>> = articleRepository.article

    init {
        viewModelScope.launch {
            articleRepository.loadArticle(state.id)
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

    fun onWordClicked(word: String) {

    }

    fun onBackPressed() {
        router.closeArticle()
    }

    @Parcelize
    class State(
        var id: Long
    ) : Parcelable
}