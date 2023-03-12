package com.aglushkov.wordteacher.shared.features.add_article.vm

import com.aglushkov.wordteacher.shared.events.*
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.repository.article.ArticleParserRepository
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
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

interface AddArticleVM: Clearable {
    val eventFlow: Flow<Event>
    // TODO: consider to move it into one StateFlow<State>
    val title: StateFlow<String>
    val titleErrorFlow: Flow<StringDesc?>
    val text: StateFlow<String>
    val needToCreateSet: StateFlow<Boolean>
    var state: State

    fun onTitleChanged(title: String)
    fun onTextChanged(text: String)
    fun onCancelPressed(): Job
    fun onTitleFocusChanged(hasFocus: Boolean)
    fun onCompletePressed()
    fun onNeedToCreateSetPressed()

    @Parcelize
    class State(
        var title: String? = null,
        var text: String? = null,
        var url: String? = null,
        var needToCreateSet: Boolean = false
    ): Parcelable
}

open class AddArticleVMImpl(
    private val articlesRepository: ArticlesRepository,
    private val articleParseRepository: ArticleParserRepository,
    private val cardSetsRepository: CardSetsRepository,
    private val timeSource: TimeSource,
    override var state: AddArticleVM.State
): ViewModel(), AddArticleVM {

    private val eventChannel = Channel<Event>(Channel.BUFFERED) // TODO: replace with a list of strings
    override val eventFlow = eventChannel.receiveAsFlow()

    // TODO: consider creating a dingle UIState object
    private val mutableTitle = MutableStateFlow("")
    override val title: StateFlow<String> = mutableTitle
    private val mutableTitleErrorFlow = MutableStateFlow<StringDesc?>(null)
    override val titleErrorFlow: Flow<StringDesc?> = mutableTitleErrorFlow
    override val needToCreateSet = MutableStateFlow<Boolean>(false)

    private val mutableText = MutableStateFlow("")
    override val text: StateFlow<String> = mutableText

    fun restore(state: AddArticleVM.State) {
        mutableTitle.value = state.title.orEmpty()
        mutableText.value = state.text.orEmpty()
        needToCreateSet.value = state.needToCreateSet

        state.url?.let {
            viewModelScope.launch {
                val parsedArticleRes = articleParseRepository.parse(it)
                parsedArticleRes.data()?.let {
                    it.title?.let {
                        mutableTitle.value = it
                    }

                    it.text?.let {
                        mutableText.value = it
                    }
                }
            }
        }
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
            if (needToCreateSet.value) {
                createCardSet()
            }

            createArticle()
        }
    }

    override fun onNeedToCreateSetPressed() {
        needToCreateSet.value = !needToCreateSet.value
        state.needToCreateSet = needToCreateSet.value
    }

    private fun createCardSet() = viewModelScope.launch {
        runSafely {
            cardSetsRepository.createCardSet(
                title.value,
                timeSource.timeInMilliseconds()
            )
        }
    }

    private fun createArticle() = viewModelScope.launch {
        runSafely {
            // TODO: show loading, adding might take for a while
            val article = articlesRepository.createArticle(title.value, text.value)
            eventChannel.trySend(CompletionEvent(CompletionResult.COMPLETED, CompletionData.Article(article.id)))
        }
    }

    private suspend fun runSafely(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.exception(e, TAG)
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

private const val TAG = "AddArticleVM"