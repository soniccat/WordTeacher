package com.aglushkov.wordteacher.shared.features.add_article.vm

import com.aglushkov.wordteacher.shared.events.*
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.data
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

interface AddArticleVM: Clearable {
    val eventFlow: Flow<Event>
    val uiStateFlow: Flow<Resource<UIState>>

    fun onTitleChanged(title: String)
    fun onTextChanged(text: String)
    fun onCancelPressed(): Job
    fun onTitleFocusChanged(hasFocus: Boolean)
    fun onCompletePressed()
    fun onNeedToCreateSetPressed()

    @Parcelize
    data class State(
        var title: String? = null,
        var text: String? = null,
        var uri: String? = null,
        var needToCreateSet: Boolean = false
    ): Parcelable

    data class UIState(
        val title: String,
        val titleError: StringDesc?,
        val text: String,
        val needToCreateSet: Boolean,
    )
}

open class AddArticleVMImpl(
    private val articlesRepository: ArticlesRepository,
    //private val articleParseRepository: ArticleParserRepository,
    private val contentExtractors: List<ArticleContentExtractor>,
    private val cardSetsRepository: CardSetsRepository,
    private val timeSource: TimeSource,
): ViewModel(), AddArticleVM {

    private val eventChannel = Channel<Event>(Channel.BUFFERED) // TODO: replace with a list of strings
    override val eventFlow = eventChannel.receiveAsFlow()

    // TODO: consider creating a dingle UIState object
//    private val mutableTitle = MutableStateFlow("")
//    override val title: StateFlow<String> = mutableTitle
//    private val mutableTitleErrorFlow = MutableStateFlow<StringDesc?>(null)
//    override val titleErrorFlow: Flow<StringDesc?> = mutableTitleErrorFlow
//    override val needToCreateSet = MutableStateFlow<Boolean>(false)
//    private val mutableText = MutableStateFlow("")
//    override val text: StateFlow<String> = mutableText

    override val uiStateFlow = MutableStateFlow<Resource<AddArticleVM.UIState>>(Resource.Uninitialized())

    fun restore(state: AddArticleVM.State) {
        val dataFromState = AddArticleVM.UIState(
            title = state.title.orEmpty(),
            titleError = null,
            text = state.text.orEmpty(),
            needToCreateSet = state.needToCreateSet,
        )
        val uiState = uiStateFlow.updateAndGet { uiState ->
            uiState.toLoading().copy(
                data = dataFromState
            ).run {
                if (dataFromState.text.isNotEmpty()) {
                    this.toLoaded(dataFromState)
                } else {
                    this
                }
            }
        }

        if (!uiState.isLoaded()) {
            state.uri?.let { uri ->
                extractContent(uri, dataFromState)
            } ?: run {
                uiStateFlow.update { it.toLoaded(dataFromState) }
            }
        }
    }

    private fun extractContent(uri: String, dataFromState: AddArticleVM.UIState) {
        viewModelScope.launch {
            uiStateFlow.update { it.toLoading() }
            var res: Resource<ArticleContent> = Resource.Uninitialized()
            for (extractor in contentExtractors) {
                if (extractor.canExtract(uri)) {
                    res = extractor.extract(uri).waitUntilLoadedOrError()
                }

                if (res.isLoadedOrError()) {
                    break
                }
            }

            uiStateFlow.update {
                res.transform {
                    dataFromState.copy(
                        title = it.title.orEmpty(),
                        text = it.text.orEmpty(),
                    )
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