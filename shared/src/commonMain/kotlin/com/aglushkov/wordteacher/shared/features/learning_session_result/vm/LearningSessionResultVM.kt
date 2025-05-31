package com.aglushkov.wordteacher.shared.features.learning_session_result.vm

import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.learning.vm.SessionCardResult
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.SimpleRouter
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.repository.data_loader.CardLoader
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface LearningSessionResultVM: Clearable {
    var router: LearningSessionResultRouter?

    val viewItems: StateFlow<Resource<List<BaseViewItem<*>>>>

    fun onTermClicked(item: LearningSessionTermResultViewItem)
    fun onTryAgainClicked()
    fun onCloseClicked()

    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?

    @Serializable
    data class State(
        val sessionResults: List<SessionCardResult>
    )
}

open class LearningSessionResultVMImpl(
    initialState: LearningSessionResultVM.State,
    private val cardLoader: CardLoader,
    private val idGenerator: IdGenerator,
    private val analytics: Analytics,
) : ViewModel(), LearningSessionResultVM {

    override var router: LearningSessionResultRouter? = null

    override val viewItems = MutableStateFlow<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())

    var state: LearningSessionResultVM.State = initialState

    init {
        startScreenFlow(state.sessionResults)
    }

    // Screen state flow
    private fun startScreenFlow(sessionResults: List<SessionCardResult>) {
        viewModelScope.launch {
            val cardMap = cardLoader.loadCardsUntilLoaded(
                cardIds = sessionResults.map { it.cardId },
                onLoading = {
                    viewItems.value = Resource.Loading()
                },
                onError = {
                    viewItems.value = Resource.Error(it, canTryAgain = true)
                }
            ).associateBy { it.id }

            val loadedSessionCardResults = sessionResults.mapNotNull { sessionResult ->
                cardMap[sessionResult.cardId]?.let { card ->
                    LoadedSessionCardResult(
                        card = card,
                        oldProgress = sessionResult.oldProgress,
                        newProgress = sessionResult.newProgress,
                        isRight = sessionResult.isRight,
                    )
                }
            }.sortedByDescending {
                if (it.isRight) {
                    1.0
                } else {
                    -1.0
                } * it.newProgress
            }
            viewItems.value = Resource.Loaded(buildItems(loadedSessionCardResults))
        }
    }

    private fun buildItems(results: List<LoadedSessionCardResult>): List<BaseViewItem<*>> {
        val viewItems = results.map {
            LearningSessionTermResultViewItem(
                cardId = it.card.id,
                term = it.card.term,
                newProgress = it.newProgress,
                isRight = it.isRight
            )
        }

        generateIds(viewItems)
        return viewItems
    }

    private fun generateIds(items: List<BaseViewItem<*>>) {
        generateViewItemIds(items, viewItems.value.data().orEmpty(), idGenerator)
    }

    override fun onTermClicked(item: LearningSessionTermResultViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("LearningResult.onTermClicked"))
        router?.openDefinitions(item.term)
    }

    override fun onTryAgainClicked() = cardLoader.tryLoadCardsAgain()

    override fun onCloseClicked() {
        router?.onScreenFinished(this, SimpleRouter.Result(true))
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.learning_session_result_error)
    }

    private data class LoadedSessionCardResult(
        val card: Card,
        val oldProgress: Float,
        var newProgress: Float,
        var isRight: Boolean,
    )
}
