package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.features.definitions.vm.Indent
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDefinitionViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordExampleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordPartOfSpeechViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSubHeaderViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSynonymViewItem
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.extensions.addElements
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.toStringDesc
import com.aglushkov.wordteacher.shared.repository.data_loader.CardLoader
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseWorker
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface LearningVM {
    val termState: StateFlow<TermState>
    val viewItems: StateFlow<Resource<List<BaseViewItem<*>>>>
    val titleErrorFlow: StateFlow<StringDesc?>

    fun onCheckPressed(answer: String)
    fun onTextChanged()
    fun onShowNextLetterPressed()
    fun onShowRandomLetterPressed()
    fun onTryAgainClicked()
    suspend fun onGiveUpPressed()
//    fun onBackPressed()

    fun save(): State
    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?

    data class TermState(
        val term: String = "",
        val index: Int = 0,
        val count: Int = 0
    )

    @Parcelize
    data class State (
        val cardIds: List<Long>,
        val teacherState: CardTeacher.State?
    ): Parcelable
}

open class LearningVMImpl(
    private var state: LearningVM.State,
    private val router: LearningRouter,
    private val cardLoader: CardLoader,
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val timeSource: TimeSource,
    private val idGenerator: IdGenerator
) : ViewModel(), LearningVM {

    override val termState = MutableStateFlow(LearningVM.TermState())
    override val viewItems = MutableStateFlow<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())
    override val titleErrorFlow = MutableStateFlow<StringDesc?>(null)

    private var teacher: CardTeacher? = null

    fun restore(newState: LearningVM.State) {
        state = newState

        startLearning(state.cardIds, state.teacherState)
    }

    // Screen state flow
    private fun startLearning(cardIds: List<Long>, teacherState: CardTeacher.State?) {
        viewModelScope.launch {
            val cards = cardLoader.loadCardsUntilLoaded(
                cardIds = cardIds,
                onLoading = {
                    viewItems.value = Resource.Loading()
                },
                onError = {
                    viewItems.value = Resource.Error(it, canTryAgain = true)
                }
            )

            val teacher = createTeacher(cards, teacherState)
            var sessionResults: List<SessionCardResult>? = null
            do {
                sessionResults = teacher.runSession { cardCount, sessionCards ->
                    sessionCards.collectIndexed { index, card ->
                        termState.update { it.copy(
                            term = card.term,
                            index = index,
                            count = cardCount
                        ) }
                        viewItems.value = Resource.Loaded(buildCardItem(card))
                    }
                }

                if (sessionResults != null) {
                    router.openSessionResult(sessionResults)
                }
            } while (sessionResults != null)

            router.closeLearning()
        }
    }

    override fun save(): LearningVM.State {
        state = state.copy(teacherState = teacher?.save())
        return state
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

    override fun onCheckPressed(answer: String) {
        viewModelScope.launch {
            checkInput(answer)
        }
    }

    private suspend fun checkInput(answer: String) {
        val teacher = teacher ?: return

        val isRight = teacher.onCheckInput(answer)
        titleErrorFlow.value = if (isRight) {
            null
        } else {
            StringDesc.Resource(MR.strings.learning_wrong_input)
        }
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
        cardLoader.tryLoadCardsAgain()
    }

//    override fun onBackPressed() {
//        router.closeLearning()
//    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.learning_error)
    }
}

private const val TERM_REPLACEMENT = "__"
