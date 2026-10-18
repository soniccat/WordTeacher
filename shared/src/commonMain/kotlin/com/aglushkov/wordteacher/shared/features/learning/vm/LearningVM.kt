package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.Indent
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordAntonymViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordAudioFilesViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDefinitionViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordExampleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordPartOfSpeechViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSubHeaderViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSynonymViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.toPartsOfSpeechFilter
import com.aglushkov.wordteacher.shared.features.definitions.vm.toViewItemAudioFile
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM.Hint
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM.State.Companion.AllCards
import com.aglushkov.wordteacher.shared.general.AudioService
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.SimpleRouter
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.extensions.addElements
import com.aglushkov.wordteacher.shared.general.extensions.collectUntilLoaded
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Color
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import com.aglushkov.wordteacher.shared.general.resource.buildSimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.settings.HintType
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.general.settings.isHintClosed
import com.aglushkov.wordteacher.shared.general.settings.setHintClosed
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.toStringResource
import wordteacher.shared.generated.resources.Res
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import org.jetbrains.compose.resources.StringResource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import wordteacher.shared.generated.resources.learning_delete_hint
import wordteacher.shared.generated.resources.learning_error
import wordteacher.shared.generated.resources.learning_instruction_challenge_match
import wordteacher.shared.generated.resources.learning_wrong_input
import wordteacher.shared.generated.resources.word_section_examples
import wordteacher.shared.generated.resources.word_section_synonyms
import kotlin.collections.fold

interface LearningVM: Clearable {
    var router: LearningRouter?

    val challengeState: StateFlow<Resource<Challenge>>
    val titleErrorFlow: StateFlow<StringResourceWrapper?>
    val canShowHint: StateFlow<Boolean>
    val hintString: StateFlow<Hint?>
    val playSoundOnChallengeCompletion: StateFlow<Boolean>
    val needShowTestHint: StateFlow<Boolean>

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
    fun onOpenDefinitionsClicked()
    fun onClosePressed()
    fun onAudioFileClicked(audioFile: WordAudioFilesViewItem.AudioFile)
    fun onPlaySoundOnChallengeCompletionClicked()
    fun onShowDeleteCardHintClicked()
    fun onDeleteCardClicked()
    fun onHintHidden(hintType: HintType)

    fun save(): State
    fun getErrorText(res: Resource<*>): StringResourceWrapper?

    data class Hint(
        val id: Int, // to bypass unique check
        val value: StringResourceWrapper,
    )

    sealed interface Challenge {
        data class Match(
            val instruction: StringResourceWrapper,
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
            val audioFiles: WordAudioFilesViewItem?,
        ): Challenge

        data class Type(
            val term: String,
            val index: Int,
            val count: Int,
            val termViewItems: List<BaseViewItem<*>>,
            val audioFiles: WordAudioFilesViewItem?,
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

        fun audioFiles(): WordAudioFilesViewItem?  = when(this) {
            is Test -> audioFiles
            is Type -> audioFiles
            else -> null
        }
    }

    @Serializable
    data class State (
        val cardSetId: Long,
        val cardIds: List<Long> = emptyList(),
        val teacherState: CardTeacher.State? = null
    ) {
        companion object {
            val AllCards = -1L
        }
    }
}

open class LearningVMImpl(
    restoredState: LearningVM.State,
    private val databaseCardWorker: DatabaseCardWorker,
    private val timeSource: TimeSource,
    private val idGenerator: IdGenerator,
    private val analytics: Analytics,
    private val audioService: AudioService,
    private val settings: SettingStore
) : ViewModel(), LearningVM {

    override var router: LearningRouter? = null
    var state: LearningVM.State = restoredState

    override val challengeState = MutableStateFlow<Resource<LearningVM.Challenge>>(Resource.Uninitialized())
    override val titleErrorFlow = MutableStateFlow<StringResourceWrapper?>(null)
    override val canShowHint = MutableStateFlow(true)
    override val hintString = MutableStateFlow<Hint?>(null)
    override val playSoundOnChallengeCompletion = MutableStateFlow(
        settings.boolean(SETTING_PLAY_SOUND_ON_CHALLENGE_COMPLETION, true)
    )
    override val needShowTestHint = MutableStateFlow(!settings.isHintClosed(HintType.LearningTestHint))
    private val matchColorMap: MutableMap<Int, Int> = mutableMapOf() // selection group to color index from MatchColors

    private var teacher: CardTeacher? = null

    private var cardRepository = buildSimpleResourceRepository<List<Card>, LearningVM.State> { state ->
        databaseCardWorker.databaseWorker.run {
            (
                if (state.cardIds.isNotEmpty()) {
                    it.cards.selectCards(state.cardIds)
                } else {
                    when (state.cardSetId) {
                        AllCards -> it.cards.selectAllCards()
                        else -> it.cards.selectCards(state.cardSetId)
                    }
                }
            ).executeAsList().filter {
                it.progress.isReadyToLearn(timeSource)
            }
        }
    }

    init {
        viewModelScope.launch {
            cardRepository.launchLoad(state)
        }
        startLearning(state)

        // update settings
        viewModelScope.launch {
            playSoundOnChallengeCompletion.onEach {
                settings[SETTING_PLAY_SOUND_ON_CHALLENGE_COMPLETION] = it
            }.collect()
        }
    }

    override fun save(): LearningVM.State {
        state = state.copy(teacherState = teacher?.save())
        return state
    }

    // Screen state flow
    private fun startLearning(aState: LearningVM.State) = viewModelScope.launch {
        challengeState.update { Resource.Loading() }

        // TODO: consider updating span priority to calculate required card spans first and skip not required for now
        addClearable(databaseCardWorker.updateSpansAndStartEditing())

        val cards = cardRepository.stateFlow.collectUntilLoaded().data().orEmpty()
        state = state.copy(cardIds = cards.map { it.id })

        val teacher = createTeacher(cards, aState.teacherState)
        launch { // start observing hint string
            teacher.hintString.map {
                Hint(
                    id = 0,
                    value = StringResourceWrapper.Raw(
                        it.fold("") { str, char ->
                            "$str $char"
                        }
                    )
                )
            }.collect(hintString)
        }
        launch { // bind canShowHint
            teacher.hintShowCount.map { it < 2 }.collect(canShowHint)
        }

        var sessionResults: List<SessionCardResult>? = null
        do {
            sessionResults = teacher.runSession { matchPairs, testCards, sessionCards ->
                analytics.send(AnalyticEvent.createActionEvent("Learning.startSession"))

                // match session
                if (matchPairs != null) {
                    analytics.send(AnalyticEvent.createActionEvent("Learning.session.match.started"))
                    matchPairs.collect { pairs ->
                        updateMatchColorMap(pairs)
                        challengeState.update {
                            Resource.Loaded(
                                LearningVM.Challenge.Match(
                                    instruction = StringResourceWrapper.String(Res.string.learning_instruction_challenge_match),
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
                    analytics.send(AnalyticEvent.createActionEvent("Learning.session.match.completed"))
                }

                var prevAudioFile: WordTeacherWord.AudioFile? = null
                val playPrevAudioFile = {
                    prevAudioFile?.let {
                        if (playSoundOnChallengeCompletion.value) {
                            audioService.play(it.url)
                        }
                    }
                    prevAudioFile = null
                }

                // test session
                if (testCards != null) {
                    analytics.send(AnalyticEvent.createActionEvent("Learning.session.test.started"))
                    testCards.collect { (index, textCard) ->
                        playPrevAudioFile()
                        prevAudioFile = textCard.card.audioFiles.firstOrNull()
                        challengeState.update {
                            Resource.Loaded(
                                LearningVM.Challenge.Test(
                                    term = textCard.card.term,
                                    index = index,
                                    count = teacher.sessionCardCount,
                                    testOptions = textCard.options,
                                    termViewItems = buildCardItem(textCard.card),
                                    audioFiles = audioFilesViewItemFromCard(textCard.card),
                                )
                            )
                        }
                    }
                    analytics.send(AnalyticEvent.createActionEvent("Learning.session.test.completed"))
                }
                playPrevAudioFile()

                // type session
                analytics.send(AnalyticEvent.createActionEvent("Learning.session.typing.started"))
                sessionCards.collect { (index, card) ->
                    playPrevAudioFile()
                    prevAudioFile = card.audioFiles.firstOrNull()
                    challengeState.update {
                        Resource.Loaded(
                            LearningVM.Challenge.Type(
                                term = card.term,
                                index = index,
                                count = teacher.sessionCardCount,
                                termViewItems = buildCardItem(card),
                                audioFiles = audioFilesViewItemFromCard(card)
                            )
                        )
                    }
                }
                playPrevAudioFile()
                analytics.send(AnalyticEvent.createActionEvent("Learning.session.typing.completed"))
                analytics.send(AnalyticEvent.createActionEvent("Learning.completeSession"))
            }

            if (sessionResults != null) {
                router?.openLearningSessionResult(sessionResults)
            }
        } while (sessionResults != null)

        onLearningCompleted()
    }

    private fun audioFilesViewItemFromCard(card: Card) =
        if (card.audioFiles.isNotEmpty()) {
            WordAudioFilesViewItem(card.audioFiles.map {
                it.toViewItemAudioFile()
            })
        } else {
            null
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

    private fun createTeacher(cards: List<Card>, teacherState: CardTeacher.State?): CardTeacher {
        return CardTeacher(
            cards,
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
            WordPartOfSpeechViewItem(card.partOfSpeech.toStringResource(), card.partOfSpeech),
            *card.resolveDefinitionsWithHiddenTerm().mapIndexed { index, def ->
                WordDefinitionViewItem(
                    definition = def,
                    labels = if (index == 0) card.labels else emptyList()
                )
            }.toTypedArray(),
        )

        if (card.examples.isNotEmpty()) {
            val examples = card.resolveExamplesWithHiddenTerm()
            viewItems.addElements(
                WordSubHeaderViewItem(
                    StringResourceWrapper.String(Res.string.word_section_examples),
                    Indent.SMALL
                ),
                *examples.mapIndexed { index, ex ->
                    WordExampleViewItem(ex, Indent.SMALL, isLast = index == examples.lastIndex)
                }.toTypedArray(),
            )
        }

        if (card.synonyms.isNotEmpty()) {
            viewItems.addElements(
                WordSubHeaderViewItem(
                    StringResourceWrapper.String(Res.string.word_section_synonyms),
                    Indent.SMALL
                ),
                *card.synonyms.map { synonym ->
                    WordSynonymViewItem(synonym, Indent.SMALL)
                }.toTypedArray(),
                *card.antonyms.map { synonym ->
                    WordAntonymViewItem(synonym, Indent.SMALL)
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
        if (isRight) {
            analytics.send(AnalyticEvent.createActionEvent("Learning.session.typing.check.isRight"))
        } else {
            analytics.send(AnalyticEvent.createActionEvent("Learning.session.typing.check.isWrong"))
        }
        titleErrorFlow.value = if (isRight) {
            null
        } else {
            StringResourceWrapper.String(Res.string.learning_wrong_input)
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
        analytics.send(AnalyticEvent.createActionEvent("Learning.hintAskedPressed"))
        viewModelScope.launch {
            teacher?.onHintAsked()
        }
    }

    override fun onGiveUpPressed() {
        analytics.send(AnalyticEvent.createActionEvent("Learning.giveUpPressed"))
        viewModelScope.launch {
            teacher?.onGiveUp()
        }
    }

    override fun onTryAgainClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Learning.onTryAgainClicked"))
        viewModelScope.launch {
            cardRepository.launchLoad(state)
        }
    }

    override fun onClosePressed() {
        router?.onScreenFinished(this, SimpleRouter.Result(true))
    }

    private fun onLearningCompleted() {
        analytics.send(AnalyticEvent.createActionEvent("Learning.learningCompleted"))
        router?.onScreenFinished(this, SimpleRouter.Result(false))
    }

    override fun onAudioFileClicked(audioFile: WordAudioFilesViewItem.AudioFile) {
        analytics.send(AnalyticEvent.createActionEvent("Learning.onAudioFileClicked"))
        audioService.play(audioFile.url)
        viewModelScope.launch {
            teacher?.countWrongAnswer()
        }
    }

    override fun onPlaySoundOnChallengeCompletionClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Learning.onPlaySoundOnTypingCompletionClicked"))
        playSoundOnChallengeCompletion.update { !it }
    }

    override fun onOpenDefinitionsClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Learning.onOpenDefinitionsClicked"))
        val card = teacher?.currentTestCard?.second?.card ?: teacher?.currentCard?.second ?: return
        viewModelScope.launch {
            teacher?.countWrongAnswer()
            router?.openDefinitions(
                DefinitionsVM.State(
                    word = card.term,
                    selectedPartsOfSpeechFilter = card.partOfSpeech.toPartsOfSpeechFilter()
                )
            )
        }
    }

    override fun onShowDeleteCardHintClicked() {
        hintString.update {
            Hint(
                id = it?.let { it.id + 1 } ?: 0,
                value = StringResourceWrapper.String(Res.string.learning_delete_hint)
            )
        }
    }

    override fun onHintHidden(hintType: HintType) {
        analytics.send(AnalyticEvent.createActionEvent("Hint_" + hintType.name))
        settings.setHintClosed(hintType)
        needShowTestHint.update { false }
    }

    override fun onDeleteCardClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Learning.onDeleteCardClicked"))

        val card = teacher?.currentTestCard?.second?.card ?: teacher?.currentCard?.second ?: return
        teacher?.onCardDeleted(card.id)
        databaseCardWorker.deleteCard(card, timeSource.timeInMilliseconds())
    }

    override fun getErrorText(res: Resource<*>): StringResourceWrapper? {
        return StringResourceWrapper.String(Res.string.learning_error)
    }
}

private val MatchColors = listOf(
    Color(0xFFCE00F1),
    Color(0xFFD8302B),
    Color(0xFF584383),
    Color(0xFFE06F1F),
    Color(0xFFDF3E6D),
    Color(0xFFEDD32F),
    Color(0xFF309449),
    Color(0xFF98F0AE),
    Color(0xFFBEAE13),
    Color(0xFFE40202),
    Color(0xFF14E2B9),
    Color(0xFF5F5D5D),
)

private const val SETTING_PLAY_SOUND_ON_CHALLENGE_COMPLETION = "playSoundOnTypingCompletion"