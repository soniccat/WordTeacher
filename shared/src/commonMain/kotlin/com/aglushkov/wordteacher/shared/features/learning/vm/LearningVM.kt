package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.features.definitions.vm.Indent
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDefinitionViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordExampleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordPartOfSpeechViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSubHeaderViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSynonymViewItem
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.extensions.addElements
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isError
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.toStringDesc
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseWorker
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

interface LearningVM {
    val viewItems: StateFlow<Resource<List<BaseViewItem<*>>>>

    suspend fun onCheckPressed(answer: String)
    fun onTextChanged()
    fun onShowNextLetterPressed()
    fun onShowRandomLetterPressed()
    fun onTryAgainClicked()
    suspend fun onGiveUpPressed()
    fun onBackPressed()

    fun save(): State
    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?

    @Parcelize
    data class State (
        val cardIds: List<Long>,
        val teacherState: CardTeacher.State?
    ): Parcelable
}

open class LearningVMImpl(
    private var state: LearningVM.State,
    private val router: LearningRouter,
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val timeSource: TimeSource,
    private val idGenerator: IdGenerator
) : ViewModel(), LearningVM {

    private val retryStatFlow = MutableStateFlow(0)
    override val viewItems = MutableStateFlow<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())

    private var teacher: CardTeacher? = null

    fun restore(newState: LearningVM.State) {
        state = newState

        startLearning(state.cardIds)
    }

    fun startLearning(id: List<Long>) {
        viewModelScope.launch {
            val cards = loadCardsUntilLoaded(
                cardIds = state.cardIds,
                onLoading = {
                    viewItems.value = Resource.Loading()
                },
                onError = {
                    viewItems.value = Resource.Error(it, canTryAgain = true)
                }
            )
            val teacher = createTeacher(cards, state.teacherState)
            val result = teacher.runSession { sessionCards ->
                sessionCards.collect { card ->
                    viewItems.value = Resource.Loaded(buildCardItem(card))
                }
            }
        }
    }

    override fun save(): LearningVM.State {
        state = state.copy(teacherState = teacher?.save())
        return state
    }

    private suspend fun loadCardsUntilLoaded(
        cardIds: List<Long>,
        onLoading: () -> Unit,
        onError: (e: Throwable) -> Unit
    ): List<Card> {
        while (true) {
            var res: Resource<List<Card>>? = Resource.Uninitialized()
            loadCards(cardIds).collect {
                res = it
                when (it) {
                    is Resource.Loading -> onLoading()
                    is Resource.Error -> onError(it.throwable)
                    else -> { }
                }
            }

            val safeRes = res
            if (safeRes is Resource.Loaded) {
                return safeRes.data
            } else if (safeRes.isError()) {
                // wait for user interaction
                retryStatFlow.first()
            }
        }
    }

    private suspend fun loadCards(cardIds: List<Long>): Flow<Resource<List<Card>>> = flow {
        emit(Resource.Loading())
        try {
            val cards = databaseWorker.run {
                database.cards.selectCards(cardIds).executeAsList()
            }
            emit(Resource.Loaded(cards))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            emit(Resource.Error(e, canTryAgain = true))
        }
    }

    private fun createTeacher(cards: List<Card>, teacherState: CardTeacher.State?): CardTeacher {
        return CardTeacher(
            cards,
            database,
            databaseWorker,
            timeSource,
            viewModelScope
        ).also { aTeacher ->
            teacherState?.let { teacherState ->
                aTeacher.restore(teacherState)
            }
            teacher = aTeacher
        }
    }

    private fun buildCardItem(card: Card): List<BaseViewItem<*>> {
        val viewItems = mutableListOf(
            WordPartOfSpeechViewItem(card.partOfSpeech.toStringDesc()),
            *card.definitions.map { def ->
                WordDefinitionViewItem(definition = def.replace(card.term, TERM_REPLACEMENT))
            }.toTypedArray(),
        )

        if (card.examples.isNotEmpty()) {
            viewItems.addElements(
                WordSubHeaderViewItem(
                    StringDesc.Resource(MR.strings.word_section_examples),
                    Indent.SMALL
                ),
                *card.examples.map { ex ->
                    WordExampleViewItem(ex.replace(card.term, TERM_REPLACEMENT), Indent.SMALL)
                }.toTypedArray(),
            )
        }

        if (card.synonyms.isNotEmpty()) {
            viewItems.addElements(
                WordSubHeaderViewItem(
                    StringDesc.Resource(MR.strings.word_section_synonyms),
                    Indent.SMALL
                ),
                *card.synonyms.map { synonym ->
                    WordSynonymViewItem(synonym.replace(card.term, TERM_REPLACEMENT), Indent.SMALL)
                }.toTypedArray()
            )
        }

        generateIds(viewItems)
        return viewItems
    }

    private fun generateIds(items: List<BaseViewItem<*>>) {
        generateViewItemIds(items, viewItems.value.data().orEmpty(), idGenerator)
    }

    override suspend fun onCheckPressed(answer: String) {
        teacher?.onCheckInput(answer)
    }

    override fun onTextChanged() {
        TODO("Not yet implemented")
    }

    override fun onShowNextLetterPressed() {
        TODO("Not yet implemented")
    }

    override fun onShowRandomLetterPressed() {
        TODO("Not yet implemented")
    }

    override suspend fun onGiveUpPressed() {
        teacher?.onGiveUp()
    }

    override fun onTryAgainClicked() {
        retryStatFlow.value++
    }

    override fun onBackPressed() {
        router.closeLearning()
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.learning_error)
    }
}

private const val TERM_REPLACEMENT = "__"
