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
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.toStringDesc
import com.aglushkov.wordteacher.shared.repository.data_loader.CardLoader
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import dev.icerock.moko.graphics.Color
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface LearningVM: Clearable {
    var router: LearningRouter?

    val challengeState: StateFlow<Resource<Challenge>>
    val titleErrorFlow: StateFlow<StringDesc?>
    val canShowHint: StateFlow<Boolean>
    val hintString: StateFlow<List<Char>>

    fun onMatchTermPressed(matchRow: Challenge.MatchRow)
    fun onMatchExamplePressed(matchRow: Challenge.MatchRow)
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
    fun getErrorText(res: Resource<*>): StringDesc?

    sealed interface Challenge {
        data class Match(
            val rows: List<MatchRow>
        ): Challenge

        data class MatchRow(
            val id: Int,
            val termColor: Color?,
            val exampleColor: Color?,
            val matchPair: MatchSession.MatchPair,
        )

        data class Test(
            val term: String,
            val index: Int,
            val count: Int,
            val testOptions: List<String>,
            val termViewItems: List<BaseViewItem<*>>,
        ): Challenge

        data class Type(
            val term: String,
            val index: Int,
            val count: Int,
            val termViewItems: List<BaseViewItem<*>>,
        ): Challenge

        fun index(): Int = when(this) {
            is Test -> index
            is Type -> index
            else -> 0
        }

        fun count(): Int = when(this) {
            is Test -> count
            is Type -> count
            else -> 0
        }

        fun termViewItems(): List<BaseViewItem<*>> = when(this) {
            is Test -> termViewItems
            is Type -> termViewItems
            else -> emptyList()
        }
    }

    @Parcelize
    data class State (
        val cardIds: List<Long>,
        val teacherState: CardTeacher.State?
    ): Parcelable
}

open class LearningVMImpl(
    private var state: LearningVM.State,
    private val cardLoader: CardLoader,
    private val databaseCardWorker: DatabaseCardWorker,
    private val timeSource: TimeSource,
    private val idGenerator: IdGenerator
) : ViewModel(), LearningVM {

    override var router: LearningRouter? = null

    override val challengeState = MutableStateFlow<Resource<LearningVM.Challenge>>(Resource.Uninitialized())
    override val titleErrorFlow = MutableStateFlow<StringDesc?>(null)
    override val canShowHint = MutableStateFlow(true)
    override val hintString = MutableStateFlow(listOf<Char>())
    private val matchColorMap: MutableMap<Int, Int> = mutableMapOf() // selection group to color index from MatchColors

    private var teacher: CardTeacher? = null

    fun restore(newState: LearningVM.State) {
        state = newState

        startLearning(state.cardIds, state.teacherState)
    }

    override fun save(): LearningVM.State {
        state = state.copy(teacherState = teacher?.save())
        return state
    }

    override fun onCleared() {
        super.onCleared()
        stopLearning()
    }

    // Screen state flow
    private fun startLearning(cardIds: List<Long>, teacherState: CardTeacher.State?) = viewModelScope.launch {
        challengeState.update { Resource.Loading() }

        // TODO: consider updating span priority to calculate required card spans first and skip not required for now
        databaseCardWorker.updateSpansAndStartEditing()

        // Need to load cards first
        val cards = cardLoader.loadCardsUntilLoaded(
            cardIds = cardIds,
            onLoading = {
                //viewItems.value = Resource.Loading()
            },
            onError = { throwable ->
                challengeState.update { Resource.Error(throwable, canTryAgain = true) }
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
            sessionResults = teacher.runSession { cardCount, matchPairs, testCards, sessionCards ->
                // match session
                matchPairs?.collect { pairs ->
                    updateMatchColorMap(pairs)
                    challengeState.update {
                        Resource.Loaded(
                            LearningVM.Challenge.Match(
                                rows = pairs.mapIndexed { index, matchPair ->
                                    LearningVM.Challenge.MatchRow(
                                        index,
                                        resolveColorForGroup(matchPair.termSelection.group),
                                        resolveColorForGroup(matchPair.exampleSelection.group),
                                        matchPair,
                                    )
                                },
                            )
                        )
                    }
                }

                // test session
                testCards?.collectIndexed { index, testCard ->
                    challengeState.update {
                        Resource.Loaded(
                            LearningVM.Challenge.Test(
                                term = testCard.card.term,
                                index = index,
                                count = cardCount,
                                testOptions = testCard.options,
                                termViewItems = buildCardItem(testCard.card),
                            )
                        )
                    }
                }

                // type session
                sessionCards.collectIndexed { index, card ->
                    Resource.Loaded(
                        LearningVM.Challenge.Type(
                            term = card.term,
                            index = index,
                            count = cardCount,
                            termViewItems = buildCardItem(card),
                        )
                    )
                }
            }

            if (sessionResults != null) {
                router?.openSessionResult(sessionResults)
            }
        } while (sessionResults != null)

        onLearningCompleted()
    }

    private fun resolveColorForGroup(group: Int): Color? {
        if (group == -1) {
            return null
        }

        val colorIndex = matchColorMap[group] ?: return null
        return MatchColors[colorIndex]
    }

    private fun updateMatchColorMap(activePairs: List<MatchSession.MatchPair>) {
        val activeGroups = LinkedHashSet<Int>().apply {
            activePairs.onEach {
                if (it.termSelection.group != -1) {
                    add(it.termSelection.group)
                }
                if (it.exampleSelection.group != -1) {
                    add(it.exampleSelection.group)
                }
            }
        }

        // remove stale groups
        matchColorMap.keys.toList().onEach { group ->
            if (!activeGroups.contains(group)) {
                matchColorMap.remove(group)
            }
        }

        // bind new groups with colors
        activeGroups.onEach {
            if (!matchColorMap.keys.contains(it)) {
                matchColorMap[it] = findAnotherColorIndex(matchColorMap.values)
            }
        }
    }

    private fun findAnotherColorIndex(excludedColorIndexes: Collection<Int>): Int {
        for (i in MatchColors.indices) {
            if (!excludedColorIndexes.contains(i)) {
                return i
            }
        }

        return 0
    }

    private fun stopLearning() {
        databaseCardWorker.endEditing()
    }

    private fun createTeacher(cards: List<Card>, teacherState: CardTeacher.State?): CardTeacher {
        return CardTeacher(
            cards.shuffled(),
            databaseCardWorker,
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
        generateViewItemIds(items, challengeState.value.data()?.termViewItems().orEmpty(), idGenerator)
    }

    override fun onMatchTermPressed(matchRow: LearningVM.Challenge.MatchRow) {
        teacher?.onMatchTermSelected(matchRow.matchPair)
    }

    override fun onMatchExamplePressed(matchRow: LearningVM.Challenge.MatchRow) {
        teacher?.onMatchExampleSelected(matchRow.matchPair)
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

    override fun getErrorText(res: Resource<*>): StringDesc? {
        return StringDesc.Resource(MR.strings.learning_error)
    }
}

private val MatchColors = listOf(
    Color(0xCE00F1FF),
    Color(0xD8302BFF),
    Color(0x584383FF),
    Color(0xE06F1FFF),
    Color(0xDF3E6DFF),
    Color(0xEDD32FFF),
    Color(0x309449FF),
    Color(0x98F0AEFF),
    Color(0xBEAE13FF),
    Color(0xE40202FF),
    Color(0x14E2B9FF),
    Color(0x5F5D5DFF),
)
