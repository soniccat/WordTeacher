package com.aglushkov.wordteacher.shared.features.add_article

import com.aglushkov.wordteacher.shared.events.CompletionEvent
import com.aglushkov.wordteacher.shared.events.CompletionResult
import com.aglushkov.wordteacher.shared.events.ErrorEvent
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class AddArticleVM(
    private val articlesRepository: ArticlesRepository,
    val state: State
): ViewModel() {

    private val mutableEventFlow = MutableSharedFlow<Event>(
        replay = Int.MAX_VALUE,
        extraBufferCapacity = Int.MAX_VALUE
    )
    val eventFlow: SharedFlow<Event> = mutableEventFlow

    private val mutableTitle = MutableStateFlow("")
    val title: StateFlow<String> = mutableTitle
    private val mutableTitleErrorFlow = MutableStateFlow<StringDesc?>(null)
    val titleErrorFlow: Flow<StringDesc?> = mutableTitleErrorFlow

    private val mutableText = MutableStateFlow("")
    val text: StateFlow<String> = mutableText

    init {
        mutableTitle.value = state.title.orEmpty()
        mutableText.value = state.text.orEmpty()
    }

    fun onTitleChanged(title: String) {
        mutableTitle.value = title
        updateTitleErrorFlow()
    }

    fun onTextChanged(text: String) {
        mutableText.value = text
    }

    fun onCancelPressed() = viewModelScope.launch {
        mutableEventFlow.emit(CompletionEvent(CompletionResult.CANCELLED))
    }

    fun onTitleFocusChanged(hasFocus: Boolean) {
        if (!hasFocus) {
            updateTitleErrorFlow()
        }
    }

    fun onCompletePressed() {
        updateTitleErrorFlow()
        if (mutableTitleErrorFlow.value == null) {
            createArticle()
        }
    }

    private fun createArticle() = viewModelScope.launch {
        val article = Article(
            0,
            title.value,
            Clock.System.now().toEpochMilliseconds(),
            text.value
        )

        try {
            articlesRepository.createArticle(article)
            mutableEventFlow.emit(CompletionEvent(CompletionResult.COMPLETED))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val errorText = e.message?.let {
                StringDesc.Raw(it)
            } ?: StringDesc.Resource(MR.strings.error_default)

            mutableEventFlow.emit(ErrorEvent(errorText))
        }
    }

    private fun updateTitleErrorFlow() {
        if (title.value.isBlank()) {
            mutableTitleErrorFlow.value = StringDesc.Resource(MR.strings.add_article_error_empty_title)
        } else {
            mutableTitleErrorFlow.value = null
        }
    }

    @Parcelize
    class State(
        var title: String? = null,
        var text: String? = null
    ): Parcelable
}
