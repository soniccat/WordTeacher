package com.aglushkov.wordteacher.shared.features.article_import.vm

import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
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

interface ArticleImportVM {
    val eventFlow: Flow<Event>
    val title: StateFlow<String>
    val text: StateFlow<String>
    var state: State

    fun onClosePressed()

    @Parcelize
    class State(
        var url: String
    ): Parcelable
}

open class AddArticleVMImpl(
    private val articlesRepository: ArticlesRepository,
    private val timeSource: TimeSource,
    override var state: AddArticleVM.State
): ViewModel(), AddArticleVM {

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()

    // TODO: consider creating a dingle UIState object
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
        state.title = title
        mutableTitle.value = title
        updateTitleErrorFlow()
    }

    override fun onTextChanged(text: String) {
        state.text = text
        mutableText.value = text
    }

    override fun onCancelPressed() = viewModelScope.launch {
        eventChannel.trySend(CompletionEvent(CompletionResult.CANCELLED))
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
        try {
            // TODO: show loading, adding might take for a while
            articlesRepository.createArticle(title.value, text.value)
            eventChannel.trySend(CompletionEvent(CompletionResult.COMPLETED))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val errorText = e.message?.let {
                StringDesc.Raw(it)
            } ?: StringDesc.Resource(MR.strings.error_default)

            eventChannel.trySend(ErrorEvent(errorText))
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
