package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.features.definitions.vm.Indent
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDefinitionViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordExampleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordPartOfSpeechViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSubHeaderViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSynonymViewItem
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.SimpleRouter
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.extensions.addElements
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface LearningVM: Clearable {
    var router: LearningRouter?

    val termState: StateFlow<TermState>
    val viewItems: StateFlow<Resource<List<BaseViewItem<*>>>>
    val titleErrorFlow: StateFlow<StringDesc?>
    val canShowHint: StateFlow<Boolean>
    val hintString: StateFlow<List<Char>>

    fun onTestOptionPressed(answer: String)
    fun onCheckPressed(answer: String)
    fun onTextChanged()
    fun onShowNextLetterPressed()
    fun onShowRandomLetterPressed()
    fun onTryAgainClicked()
    fun onHintAskedPressed()
    fun onGiveUpPressed()
    fun onClosePressed()

    fun save(): State
    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?

    data class TermState(
        val term: String = "",
        val index: Int = 0,
        val count: Int = 0,
        val testOptions: List<String> = emptyList() // for testSession
    )

    @Parcelize
    data class State (
        val cardIds: List<Long>,
        val teacherState: CardTeacher.State?
    ): Parcelable
}

open class LearningVMImpl(
    private var state: LearningVM.State,
    private val cardLoader: CardLoader,
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val timeSource: TimeSource,
    private val idGenerator: IdGenerator
) : ViewModel(), LearningVM {

    override var router: LearningRouter? = null

    override val termState = MutableStateFlow(LearningVM.TermState())
    override val viewItems = MutableStateFlow<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())
    override val titleErrorFlow = MutableStateFlow<StringDesc?>(null)
    override val canShowHint = MutableStateFlow(true)
    override val hintString = MutableStateFlow(listOf<Char>())

    private var teacher: CardTeacher? = null

    fun restore(newState: LearningVM.State) {
        state = newState

        startLearning(state.cardIds, state.teacherState)
    }

    // Screen state flow
    private fun startLearning(cardIds: List<Long>, teacherState: CardTeacher.State?) {
        viewModelScope.launch {
            // Need to load cards first
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
            launch { // start observing hint string
                teacher.hintString.collect(hintString)
            }
            launch { // bind canShowHint
                teacher.hintShowCount.map { it < 2 }.collect(canShowHint)
            }

            var sessionResults: List<SessionCardResult>? = null
            do {
                sessionResults = teacher.runSession { cardCount, testCards, sessionCards ->
                    // test session
                    testCards?.collectIndexed { index, testCard ->
                        termState.update { it.copy(
                            term = testCard.card.term,
                            index = index,
                            count = cardCount,
                            testOptions = testCard.options
                        ) }
                        viewItems.value = Resource.Loaded(buildCardItem(testCard.card))
                    }

                    // type session
                    sessionCards.collectIndexed { index, card ->
                        termState.update { it.copy(
                            term = card.term,
                            index = index,
                            count = cardCount,
                            testOptions = emptyList()
                        ) }
                        viewItems.value = Resource.Loaded(buildCardItem(card))
                    }
                }

                if (sessionResults != null) {
                    router?.openSessionResult(sessionResults)
                }
            } while (sessionResults != null)

            onLearningCompleted()
        }
    }

    override fun save(): LearningVM.State {
        state = state.copy(teacherState = teacher?.save())
        return state
    }

    private fun createTeacher(cards: List<Card>, teacherState: CardTeacher.State?): CardTeacher {
        return CardTeacher(
            cards.shuffled(),
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
            WordPartOfSpeechViewItem(card.partOfSpeech.toStringDesc(), card.partOfSpeech),
            *card.resolveDefinitionsWithHiddenTerm().map { def ->
                WordDefinitionViewItem(definition = def)
            }.toTypedArray(),
        )

        if (card.examples.isNotEmpty()) {
            viewItems.addElements(
                WordSubHeaderViewItem(
                    StringDesc.Resource(MR.strings.word_section_examples),
                    Indent.SMALL
                ),
                *card.resolveExamplesWithHiddenTerm().map { ex ->
                    WordExampleViewItem(ex, Indent.SMALL)
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
                    WordSynonymViewItem(synonym, Indent.SMALL)
                }.toTypedArray()
            )
        }

        generateIds(viewItems)
        return viewItems
    }

    private fun generateIds(items: List<BaseViewItem<*>>) {
        generateViewItemIds(items, viewItems.value.data().orEmpty(), idGenerator)
    }

    override fun onTestOptionPressed(answer: String) {
        teacher?.onTestOptionSelected(answer)
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

    override fun onHintAskedPressed() {
        viewModelScope.launch {
            teacher?.onHintAsked()
        }
    }

    override fun onGiveUpPressed() {
        viewModelScope.launch {
            teacher?.onGiveUp()
        }
    }

    override fun onTryAgainClicked() {
        cardLoader.tryLoadCardsAgain()
    }

    override fun onClosePressed() {
        router?.onScreenFinished(this, SimpleRouter.Result(true))
    }

    private fun onLearningCompleted() {
        router?.onScreenFinished(this, SimpleRouter.Result(false))
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.learning_error)
    }
}

