package com.aglushkov.wordteacher.shared.features.add_article.vm

import com.aglushkov.resources.desc.Raw
import com.aglushkov.resources.desc.Resource
import com.aglushkov.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.events.CompletionEvent
import com.aglushkov.wordteacher.shared.events.CompletionResult
import com.aglushkov.wordteacher.shared.events.ErrorEvent
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

interface AddArticleVM {
    val eventFlow: Flow<Event>
    val title: StateFlow<String>
    val titleErrorFlow: Flow<StringDesc?>
    val text: StateFlow<String>
    var state: AddArticleVM.State

    fun onTitleChanged(title: String)
    fun onTextChanged(text: String)
    fun onCancelPressed(): Job
    fun onTitleFocusChanged(hasFocus: Boolean)
    fun onCompletePressed()

    @Parcelize
    class State(
        var title: String? = null,
        var text: String? = null
    ): Parcelable
}

open class AddArticleVMImpl(
    private val articlesRepository: ArticlesRepository,
    private val timeSource: TimeSource,
    override var state: AddArticleVM.State
): ViewModel(), AddArticleVM {

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()

    private val mutableTitle = MutableStateFlow("")
    override val title: StateFlow<String> = mutableTitle
    private val mutableTitleErrorFlow = MutableStateFlow<StringDesc?>(null)
    override val titleErrorFlow: Flow<StringDesc?> = mutableTitleErrorFlow

    private val mutableText = MutableStateFlow("")
    override val text: StateFlow<String> = mutableText

    init {
        restore(state)
    }

    fun restore(state: AddArticleVM.State) {
        mutableTitle.value = state.title.orEmpty()
        mutableText.value = state.text.orEmpty()
    }

    override fun onTitleChanged(title: String) {
        mutableTitle.value = title
        updateTitleErrorFlow()
    }

    override fun onTextChanged(text: String) {
        mutableText.value = text
    }

    override fun onCancelPressed() = viewModelScope.launch {
        eventChannel.offer(CompletionEvent(CompletionResult.CANCELLED))
    }

    override fun onTitleFocusChanged(hasFocus: Boolean) {
        if (!hasFocus) {
            updateTitleErrorFlow()
        }
    }

    override fun onCompletePressed() {
        updateTitleErrorFlow()
        if (mutableTitleErrorFlow.value == null) {
            createArticle()
        }
    }

    private fun createArticle() = viewModelScope.launch {
        val article = Article(
            0,
            title.value,
            timeSource.getTimeInMilliseconds(),
            text.value
        )

        try {
            // TODO: show loading, adding might take for a while
            articlesRepository.createArticle(article)
            eventChannel.offer(CompletionEvent(CompletionResult.COMPLETED))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val errorText = e.message?.let {
                StringDesc.Raw(it)
            } ?: StringDesc.Resource(MR.strings.error_default)

            eventChannel.offer(ErrorEvent(errorText))
        }
    }

    private fun updateTitleErrorFlow() {
        if (title.value.isBlank()) {
            mutableTitleErrorFlow.value = StringDesc.Resource(MR.strings.add_article_error_empty_title)
        } else {
            mutableTitleErrorFlow.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventChannel.cancel()
    }
}
