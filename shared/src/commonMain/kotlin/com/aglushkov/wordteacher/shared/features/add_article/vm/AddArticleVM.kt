package com.aglushkov.wordteacher.shared.features.add_article.vm

import com.aglushkov.wordteacher.shared.events.*
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.extensions.updateData
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import com.aglushkov.wordteacher.shared.general.resource.onError
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface AddArticleVM: Clearable {
    val eventFlow: Flow<Event>
    val uiStateFlow: StateFlow<Resource<UIState>>
    val addingStateFlow: StateFlow<Resource<Unit>>

    fun createState(): State
    fun onTitleChanged(title: String)
    fun onTextChanged(text: String)
    fun onCancelPressed(): Job
    fun onTitleFocusChanged(hasFocus: Boolean)
    fun onCompletePressed()
    fun onNeedToCreateSetPressed()
    fun onTryAgainPressed()
    fun getErrorText(): StringDesc?

    @Parcelize
    data class State(
        val title: String? = null,
        val text: String? = null,
        val uri: String? = null,
        val needToCreateSet: Boolean = false
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
    private val contentExtractors: Array<ArticleContentExtractor>,
    private val cardSetsRepository: CardSetsRepository,
    private val timeSource: TimeSource,
): ViewModel(), AddArticleVM {

    private val eventChannel = Channel<Event>(Channel.BUFFERED) // TODO: replace with a list of strings
    override val eventFlow = eventChannel.receiveAsFlow()

    private var state = AddArticleVM.State()
    override val uiStateFlow = MutableStateFlow<Resource<AddArticleVM.UIState>>(Resource.Uninitialized())
    override val addingStateFlow = MutableStateFlow<Resource<Unit>>(Resource.Uninitialized())

    override fun createState(): AddArticleVM.State {
        val data = uiStateFlow.value.data()
        return state.copy(
            title = data?.title
        )
    }

    fun restore(state: AddArticleVM.State) {
        this.state = state

        val dataFromState = AddArticleVM.UIState(
            title = state.title.orEmpty(),
            titleError = null,
            text = state.text.orEmpty(),
            needToCreateSet = state.needToCreateSet,
        )

        state.uri?.let { uri ->
            extractContent(uri, dataFromState)
        } ?: run {
            uiStateFlow.update { it.toLoaded(dataFromState) }
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

            uiStateFlow.update { uiStateRes ->
                res.mapTo(uiStateRes) {
                    dataFromState.copy(
                        title = it.title.orEmpty(),
                        text = it.text.orEmpty(),
                    )
                }
            }
        }
    }

    override fun onTryAgainPressed() {
        val uiStateData = uiStateFlow.value.data() ?: return
        val uri = this.state.uri ?: return

        extractContent(uri, uiStateData)
    }

    override fun onTitleChanged(title: String) {
        uiStateFlow.updateData { it.copy(title = title) }
        updateTitleErrorFlow()
    }

    override fun onTextChanged(text: String) {
        uiStateFlow.updateData { it.copy(text = text) }
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
        uiStateFlow.value.data()?.let { data ->
            viewModelScope.launch {
                if (data.titleError == null) {
                    loadResource {
                        if (data.needToCreateSet) {
                            createCardSet()
                        }

                        createArticle()
                        Unit
                    }.collect(addingStateFlow)

                    addingStateFlow.value.onError { e ->
                        Logger.exception(e, TAG)
                        val errorText = e.message?.let {
                            StringDesc.Raw(it)
                        } ?: StringDesc.Resource(MR.strings.error_default)

                        eventChannel.trySend(ErrorEvent(errorText))
                    }
                }
            }
        }
    }

    override fun onNeedToCreateSetPressed() {
        uiStateFlow.updateData { it.copy(needToCreateSet = !it.needToCreateSet) }
    }

    private suspend fun createCardSet() {
        uiStateFlow.value.data()?.let { data ->
            cardSetsRepository.createCardSet(
                data.title,
                timeSource.timeInMilliseconds()
            )
        }
    }

    private suspend fun createArticle() {
        uiStateFlow.value.data()?.let { data ->
            val article = articlesRepository.createArticle(data.title, data.text)
            eventChannel.trySend(
                CompletionEvent(
                    CompletionResult.COMPLETED,
                    CompletionData.Article(article.id)
                )
            )
        }
    }

    private fun updateTitleErrorFlow() {
        val data = uiStateFlow.value.data()
        if (data != null && data.title.isBlank()) {
            uiStateFlow.updateData { it.copy(titleError = StringDesc.Resource(MR.strings.add_article_error_empty_title)) }
        } else {
            uiStateFlow.updateData { it.copy(titleError = null) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventChannel.cancel()
    }

    override fun getErrorText(): StringDesc? {
        return StringDesc.Resource(MR.strings.article_error)
    }
}

private const val TAG = "AddArticleVM"