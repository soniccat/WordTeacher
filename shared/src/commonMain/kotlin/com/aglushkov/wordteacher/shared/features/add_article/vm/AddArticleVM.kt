package com.aglushkov.wordteacher.shared.features.add_article.vm

import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.events.*
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.extensions.updateLoadedData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resource.onData
import com.aglushkov.wordteacher.shared.general.resource.onError
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.res.MR
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface AddArticleVM: Clearable {
    val eventFlow: Flow<Event>
    val uiStateFlow: StateFlow<Resource<UIState>>
    val addingStateFlow: StateFlow<Resource<Article>>

    fun createState(): State
    fun onTitleChanged(title: String)
    fun onTextChanged(text: String)
    fun onCancelPressed(): Job
    fun onTitleFocusChanged(hasFocus: Boolean)
    fun onCompletePressed()
    fun onNeedToCreateSetPressed()
    fun onTryAgainPressed()
    fun getErrorText(): StringDesc?

    @Serializable
    data class State(
        val title: String? = null,
        val text: String? = null,
        val uri: String? = null,
        val needToCreateSet: Boolean = true,
        val showNeedToCreateCardSet: Boolean = true,
    )

    data class UIState(
        val title: String,
        val titleError: StringDesc?,
        val text: String,
        val needToCreateSet: Boolean,
        val showNeedToCreateCardSet: Boolean = true,
        val contentUri: String? = null
    )
}

open class AddArticleVMImpl(
    restoredState: AddArticleVM.State,
    private val articlesRepository: ArticlesRepository,
    private val contentExtractors: Array<ArticleContentExtractor>,
    private val cardSetsRepository: CardSetsRepository,
    private val timeSource: TimeSource,
    private val analytics: Analytics,
): ViewModel(), AddArticleVM {

    private val eventChannel = Channel<Event>(Channel.BUFFERED) // TODO: replace with a list of strings
    override val eventFlow = eventChannel.receiveAsFlow()

    private var state = restoredState
    override val uiStateFlow = MutableStateFlow<Resource<AddArticleVM.UIState>>(Resource.Uninitialized())
    override val addingStateFlow = MutableStateFlow<Resource<Article>>(Resource.Uninitialized())

    init {
        val dataFromState = AddArticleVM.UIState(
            title = state.title.orEmpty(),
            titleError = null,
            text = state.text.orEmpty(),
            needToCreateSet = state.needToCreateSet,
            showNeedToCreateCardSet = state.showNeedToCreateCardSet,
        )

        state.uri?.let { uri ->
            extractContent(uri, dataFromState)
        } ?: run {
            uiStateFlow.update { it.toLoaded(dataFromState) }
        }
    }

    override fun createState(): AddArticleVM.State {
        val data = uiStateFlow.value.data()
        return state.copy(
            title = data?.title
        )
    }

    private fun extractContent(uri: String, dataFromState: AddArticleVM.UIState) {
        viewModelScope.launch {
            uiStateFlow.update { it.toLoading() }
            var res: Resource<ArticleContent> = Resource.Uninitialized()
            for (extractor in contentExtractors) {
                if (extractor.canExtract(uri)) {
                    extractor.extract(uri).collect { res = it }
                }
            }

            uiStateFlow.update { uiStateRes ->
                res.map { articleContent ->
                    dataFromState.copy(
                        title = articleContent?.title.orEmpty(),
                        text = articleContent?.text.orEmpty(),
                        contentUri = uri
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
        uiStateFlow.updateLoadedData { it.copy(title = title) }
        updateTitleErrorFlow()
    }

    override fun onTextChanged(text: String) {
        uiStateFlow.updateLoadedData { it.copy(text = text) }
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
        if (addingStateFlow.value.isLoading()) {
            // TODO: support cancellation
            return
        }

        updateTitleErrorFlow()
        uiStateFlow.value.data()?.let { data ->
            viewModelScope.launch {
                if (data.titleError == null) {
                    addingStateFlow.update {
                        it.toLoading()
                    }
                    if (data.needToCreateSet) {
                        createCardSet(data.contentUri)
                    }

                    articlesRepository.createArticle(data.title, data.text).collect(addingStateFlow)

                    addingStateFlow.value.onData { article ->
                        analytics.send(
                            AnalyticEvent.createActionEvent(
                                "AddArticleVM.AddArticle",
                                mapOf("id" to article.name, "uri" to state.uri, "createSet" to state.needToCreateSet))
                        )
                        eventChannel.trySend(
                            CompletionEvent(
                                CompletionResult.COMPLETED,
                                CompletionData.Article(article.id)
                            )
                        )
                    }

                    addingStateFlow.value.onError { e ->
                        Logger.exception("AddArticleVM.createArticle", e, TAG)
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
        uiStateFlow.updateLoadedData { it.copy(needToCreateSet = !it.needToCreateSet) }
    }

    private suspend fun createCardSet(contentUri: String?) {
        uiStateFlow.value.data()?.let { data ->
            cardSetsRepository.createCardSet(
                data.title,
                timeSource.timeInMilliseconds(),
                contentUri
            )
        }
    }

    private fun updateTitleErrorFlow() {
        val data = uiStateFlow.value.data()
        if (data != null && data.title.isBlank()) {
            uiStateFlow.updateLoadedData { it.copy(titleError = StringDesc.Resource(MR.strings.add_article_error_empty_title)) }
        } else {
            uiStateFlow.updateLoadedData { it.copy(titleError = null) }
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