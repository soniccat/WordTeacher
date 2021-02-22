package com.aglushkov.wordteacher.shared.features.add_article

import com.aglushkov.wordteacher.shared.events.CompletionEvent
import com.aglushkov.wordteacher.shared.events.CompletionResult
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class AddArticleVM(
    private val articlesRepository: ArticleRepository,
    val state: State
): ViewModel() {

    // TODO: clarify parameters
    private val mutableEventFlow = MutableSharedFlow<Event>(
        replay = 100,
        extraBufferCapacity = 100
    )
    val eventFlow: SharedFlow<Event> = mutableEventFlow

    private val mutableTitle = MutableStateFlow("")
    val title: StateFlow<String> = mutableTitle
    val completeButtonEnabled: Flow<Boolean> = title.map { it.isNotBlank() }

    private val mutableText = MutableStateFlow("")
    val text: StateFlow<String> = mutableText

    init {
        mutableTitle.value = state.title.orEmpty()
        mutableText.value = state.text.orEmpty()
    }

    fun onTitleChanged(title: String) {
        mutableTitle.value = title
    }

    fun onTextChanged(text: String) {
        mutableText.value = text
    }

    suspend fun onCancelPressed() {
        mutableEventFlow.emit(CompletionEvent(CompletionResult.CANCELLED))
    }

    suspend fun onCompletePressed() = coroutineScope {
        val article = Article(
            0,
            title.value,
            Clock.System.now().toEpochMilliseconds(),
            text.value
        )

        articlesRepository.putArticle(article)
        mutableEventFlow.emit(CompletionEvent(CompletionResult.COMPLETED))
    }

    @Parcelize
    class State(
        var title: String? = null,
        var text: String? = null
    ): Parcelable
}
