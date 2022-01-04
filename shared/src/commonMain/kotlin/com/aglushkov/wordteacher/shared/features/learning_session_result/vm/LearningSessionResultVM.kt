package com.aglushkov.wordteacher.shared.features.learning_session_result.vm

import com.aglushkov.wordteacher.shared.features.learning.vm.SessionCardResult
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.repository.data_loader.CardLoader
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface LearningSessionResultVM {
    val viewItems: StateFlow<Resource<List<BaseViewItem<*>>>>

    fun onTryAgainClicked()
    fun onBackPressed()

    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?

    @Parcelize
    data class State(
        val sessionResults: List<SessionCardResult>
    ) : Parcelable
}

open class LearningSessionResultVMImpl(
    private var state: LearningSessionResultVM.State,
    private val router: LearningSessionResultRouter,
    private val cardLoader: CardLoader,
    private val idGenerator: IdGenerator
) : ViewModel(), LearningSessionResultVM {

    override val viewItems = MutableStateFlow<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())

    fun restore(state: LearningSessionResultVM.State) {
        this.state = state
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

    override fun onTryAgainClicked() = cardLoader.tryLoadCardsAgain()

    override fun onBackPressed() = router.closeSessionResult()

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
