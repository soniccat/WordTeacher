package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictWord
import com.aglushkov.wordteacher.shared.dicts.Dict
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetExpandOrCollapseViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.features.settings.vm.SETTING_GET_WORD_FROM_CLIPBOARD
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilDone
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.buildSimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.getErrorString
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import com.aglushkov.wordteacher.shared.general.resource.merge
import com.aglushkov.wordteacher.shared.general.resource.onData
import com.aglushkov.wordteacher.shared.general.resource.onLoaded
import com.aglushkov.wordteacher.shared.model.ShortCardSet
import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.toStringDesc
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.clipboard.ClipboardRepository
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyLevelAndRatio
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionHistoryRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.russhwolf.settings.boolean
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.getBooleanStateFlow
import com.russhwolf.settings.coroutines.toBlockingObservableSettings
import com.russhwolf.settings.coroutines.toBlockingSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

interface DefinitionsVM: Clearable {
    var router: DefinitionsRouter?

    fun onWordTextUpdated(newText: String)
    fun onWordSubmitted(
        word: String?,
        filter: List<WordTeacherWord.PartOfSpeech> = emptyList(),
        definitionsContext: DefinitionsContext? = null
    )
    fun onTryAgainClicked()
    fun onPartOfSpeechFilterUpdated(filter: List<WordTeacherWord.PartOfSpeech>)
    fun onPartOfSpeechFilterClicked(item: DefinitionsDisplayModeViewItem)
    fun onPartOfSpeechFilterCloseClicked(item: DefinitionsDisplayModeViewItem)
    fun onDisplayModeChanged(mode: DefinitionsDisplayMode)
    fun getErrorText(res: Resource<*>): StringDesc?
    fun onEventHandled(event: Event, withAction: Boolean)

    val wordTextValue: StateFlow<String>
    val state: State
    val events: StateFlow<Events>
    val definitions: StateFlow<Resource<List<BaseViewItem<*>>>>
    val partsOfSpeechFilterStateFlow: StateFlow<List<WordTeacherWord.PartOfSpeech>>
    val selectedPartsOfSpeechStateFlow: StateFlow<List<WordTeacherWord.PartOfSpeech>>

    // Card Sets
    val cardSets: StateFlow<Resource<List<BaseViewItem<*>>>>

    fun onOpenCardSets(item: OpenCardSetViewItem)
    fun onAddDefinitionInSet(wordDefinitionViewItem: WordDefinitionViewItem, cardSetViewItem: CardSetViewItem)
    fun onCardSetExpandCollapseClicked(item: CardSetExpandOrCollapseViewItem)

    // Suggests
    val suggests: StateFlow<Resource<List<BaseViewItem<*>>>>

    fun clearSuggests()
    fun requestSuggests(word: String)
    fun onSuggestedSearchWordClicked(item: WordSuggestByTextViewItem)
    fun onSuggestedShowAllSearchWordClicked()

    // Word history
    val wordHistory: StateFlow<Resource<List<BaseViewItem<*>>>>
    val isWordHistorySelected: StateFlow<Boolean>

    fun toggleWordHistory()
    fun onWordHistoryClicked(item: WordHistoryViewItem)

    @Serializable
    data class State(
        var word: String? = null,
    )

    data class Events (
        val cardSetUpdatedEvents: List<Event.CardSetUpdatedEvent> = listOf()
    )

    sealed interface Event {
        val text: StringDesc
        val actionText: StringDesc

        data class CardSetUpdatedEvent(
            override val text: StringDesc,
            val openText: StringDesc,
            val id: Long,
        ): Event {
            override val actionText: StringDesc
                get() = openText
        }

//        data class ShowPartsOfSpeechFilterDialogEvent(
//            val partsOfSpeech: List<WordTeacherWord.PartOfSpeech>,
//            val selectedPartsOfSpeech: List<WordTeacherWord.PartOfSpeech>,
//            override var isHandled: Boolean = false
//        ): Event {
//            override fun markAsHandled() {
//                isHandled = true
//            }
//        }
    }

    data class Settings(
        val needStoreDefinedWordInSettings: Boolean = false
    )
}

open class DefinitionsVMImpl(
    restoredState: DefinitionsVM.State,
    private val connectivityManager: ConnectivityManager,
    private val wordDefinitionRepository: WordDefinitionRepository,
    private val dictRepository: DictRepository,
    private val cardSetsRepository: CardSetsRepository,
    private val wordFrequencyGradationProvider: WordFrequencyGradationProvider,
    private val wordTeacherDictService: WordTeacherDictService,
    private val definitionsSettings: DefinitionsVM.Settings,
    private val clipboardRepository: ClipboardRepository,
    private val idGenerator: IdGenerator,
    private val analytics: Analytics,
    private val settings: FlowSettings,
    private val wordDefinitionHistoryRepository: WordDefinitionHistoryRepository,
): ViewModel(), DefinitionsVM {

    override var router: DefinitionsRouter? = null
    final override var state: DefinitionsVM.State = restoredState.copy(
        word = if (definitionsSettings.needStoreDefinedWordInSettings) {
            settings.toBlockingSettings().getStringOrNull(SETTING_LAST_DEFINED_WORD) ?: restoredState.word
        } else {
            restoredState.word
        }
    )
    override val wordTextValue = MutableStateFlow(state.word.orEmpty())
    override val events = MutableStateFlow(DefinitionsVM.Events())
    private val definitionWords = MutableStateFlow<Resource<List<WordTeacherWord>>>(Resource.Uninitialized())
    private val wordFrequency = MutableStateFlow<Resource<Double>>(Resource.Uninitialized())

    final override var selectedPartsOfSpeechStateFlow = MutableStateFlow<List<WordTeacherWord.PartOfSpeech>>(emptyList())

    override val definitions = combine(
        definitionWords,
        settings.getIntFlow(SETTING_DEFINITION_DISPLAY_MODE, SETTING_DEFINITION_DISPLAY_MODE_BY_SOURCE),
        selectedPartsOfSpeechStateFlow,
        wordFrequencyGradationProvider.gradationState,
        wordFrequency,
        transform = { wordDefinitions, displayModeIndex, partOfSpeechFilter, wordFrequencyGradation, wordFrequency ->
        //Logger.v("build view items ${wordDefinitions.data()?.size ?: 0}")
            val wordFrequencyLevelAndRatio = wordFrequencyGradation.data()?.gradationLevelAndRatio(wordFrequency.data())
            wordDefinitions.copyWith(
                buildViewItems(
                    wordDefinitions.data().orEmpty(),
                    displayModes.getOrNull(displayModeIndex) ?: DefinitionsDisplayMode.BySource,
                    partOfSpeechFilter,
                    wordDefinitions.isLoading(),
                    wordFrequencyLevelAndRatio
                )
            )
    }).stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    override val partsOfSpeechFilterStateFlow = definitionWords.map {
        it.data().orEmpty().map { word ->
            word.definitions.keys
        }.flatten().distinct()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val suggestedDictEntryRepository = buildSimpleResourceRepository<List<Dict.Index.Entry>, String> { word ->
        dictRepository.wordsStartWith(word, 40)
    }
    private val wordTextSearchRepository = buildSimpleResourceRepository<List<WordTeacherDictWord>, String> { text ->
        wordTeacherDictService.textSearch(text).toOkResponse().words.orEmpty()
    }

    private val displayModes = listOf(DefinitionsDisplayMode.BySource, DefinitionsDisplayMode.Merged)
    private var loadJob: Job? = null
    private var observeJob: Job? = null
    private var definitionsContext: DefinitionsContext? = null

    private var word: String?
        get() {
            return state.word
        }
        set(value) {
            state.word = value
        }

    private var lastHandledClipData: ClipboardRepository.Data? = null

    init {
        if (definitionsSettings.needStoreDefinedWordInSettings) {
            settings.toBlockingSettings().getStringOrNull(SETTING_LAST_DEFINED_WORD) ?: word
        } else {
            word
        }?.let {
            loadIfNeeded(it)
            requestSuggests(it)
        }

        if (definitionsSettings.needStoreDefinedWordInSettings) {
            viewModelScope.launch {
                definitionWords.collect {
                    word?.let {
                        settings.putString(SETTING_LAST_DEFINED_WORD, it)
                    }
                }
            }
        }

        viewModelScope.launch {
            combine(
                settings.getBooleanFlow(SETTING_GET_WORD_FROM_CLIPBOARD, false),
                clipboardRepository.clipData,
            ) { isEnabled, clipData ->
                if (isEnabled && lastHandledClipData != clipData && !clipData.isEmpty) {
                    lastHandledClipData = clipData
                    word = clipData.text
                    wordTextValue.update { clipData.text }
                    loadIfNeeded(clipData.text)
                    requestSuggests(clipData.text)
                }
            }.collect()
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    // Events
    override fun onWordTextUpdated(newText: String) {
        wordTextValue.update { newText }
    }

    override fun onWordSubmitted(
        word: String?,
        filter: List<WordTeacherWord.PartOfSpeech>,
        definitionsContext: DefinitionsContext?
    ) {
        wordTextValue.update { word.orEmpty() }
        selectedPartsOfSpeechStateFlow.value = filter
        this.definitionsContext = definitionsContext

        if (word == null) {
            this.word = null
        } else if (word.isNotEmpty()) {
            loadIfNeeded(word)
        }
    }

    override fun onPartOfSpeechFilterUpdated(filter: List<WordTeacherWord.PartOfSpeech>) {
        analytics.send(AnalyticEvent.createActionEvent("Definitions.partOfSpeechFilterUpdated"))
        selectedPartsOfSpeechStateFlow.value = filter
    }

    override fun onPartOfSpeechFilterClicked(item: DefinitionsDisplayModeViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("Definitions.partOfSpeechFilterClicked"))
//        viewModelScope.launch {
//            eventChannel.trySend(
//                ShowPartsOfSpeechFilterDialogEvent(
//                    selectedPartsOfSpeechStateFlow.value,
//                    partsOfSpeechFilterStateFlow.value
//                )
//            )
//        }
    }

    override fun onPartOfSpeechFilterCloseClicked(item: DefinitionsDisplayModeViewItem) {
        selectedPartsOfSpeechStateFlow.value = emptyList()
    }

    override fun onDisplayModeChanged(mode: DefinitionsDisplayMode) {
        analytics.send(AnalyticEvent.createActionEvent("Definitions.displayModeChanged"))
        viewModelScope.launch {
            settings.putInt(SETTING_DEFINITION_DISPLAY_MODE, mode.ordinal)
        }
    }

    override fun onTryAgainClicked() {
        word?.let {
            wordDefinitionRepository.clear(it)
            loadIfNeeded(it)
        }
    }

    // Actions

    private fun loadIfNeeded(word: String) {
        this.word = word
        isWordHistorySelected.update { false }

        val wordRes = wordDefinitionRepository.obtainStateFlow(word).value
        if (wordRes.isLoading()) {
            return
        }
        wordRes.onLoaded(
            block = {
                val flattenedValue = it.map { it.second }.flatten()
                definitionWords.update {
                    it.toLoaded(flattenedValue).bumpVersion()
                }
                wordDefinitionHistoryRepository.put(word)
            },
            elseBlock = {
                load(word)
            },
        )
    }

    private fun load(word: String) {
        val tag = "DefinitionsVM.load"
        Logger.v("Start Loading $word", tag)

        observeJob?.cancel()
        loadJob?.cancel()

        loadJob = viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            Logger.e("Load Word exception for $word ${e.message}", tag)
        }) {
            launch(Dispatchers.Default) {
                loadResource {
                    wordFrequencyGradationProvider.resolveFrequencyForWord(word)
                }.collect(wordFrequency)
            }

            wordDefinitionRepository.define(word, false).map {
                it.mapLoadedData { it.map { it.second }.flatten() }
            }.collect(definitionWords)
            Logger.v("Finish Loading $word", tag)
        }

        observeJob = viewModelScope.launch {
            wordDefinitionRepository.obtainStateFlow(word).collect {
                if (it is Resource.Loaded && it.canLoadNextPage) {
                    load(word) // load new definitions from new services or dicts
                }
            }
        }
    }

    private fun buildViewItems(
        words: List<WordTeacherWord>,
        displayMode: DefinitionsDisplayMode,
        partsOfSpeechFilter: List<WordTeacherWord.PartOfSpeech>,
        isLoading: Boolean,
        wordFrequencyLevelAndRatio: WordFrequencyLevelAndRatio?,
    ): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()
        if (words.isNotEmpty()) {
            items.add(DefinitionsDisplayModeViewItem(
                getPartOfSpeechChipText(partsOfSpeechFilter),
                partsOfSpeechFilter.isNotEmpty(),
                displayModes,
                displayModes.indexOf(displayMode)
            ))
            items.add(WordDividerViewItem())
        }

        when (displayMode) {
            DefinitionsDisplayMode.Merged -> addMergedWords(words, partsOfSpeechFilter, items, wordFrequencyLevelAndRatio)
            else -> addWordsGroupedBySource(words, partsOfSpeechFilter, items, wordFrequencyLevelAndRatio)
        }

        if (items.isNotEmpty() && isLoading) {
            items += WordLoadingViewItem()
        }

        generateIds(items)
        return items
    }

    private fun getPartOfSpeechChipText(
        partOfSpeechFilter: List<WordTeacherWord.PartOfSpeech>,
    ): StringDesc {
        return if (partOfSpeechFilter.isEmpty()) {
            StringDesc.Resource(MR.strings.definitions_add_filter)
        } else {
            val firstFilter = partOfSpeechFilter.first()
            firstFilter.toStringDesc()
        }
    }

    private fun generateIds(items: MutableList<BaseViewItem<*>>) {
        generateViewItemIds(items, definitions.value.data().orEmpty(), idGenerator)
    }

    private fun addMergedWords(
        words: List<WordTeacherWord>,
        partsOfSpeechFilter: List<WordTeacherWord.PartOfSpeech>,
        items: MutableList<BaseViewItem<*>>,
        wordFrequencyLevelAndRatio: WordFrequencyLevelAndRatio?,
    ) {
        words.groupBy { it.word }
            .map {
                val mergedWord = mergeWords(it.value, partsOfSpeechFilter)
                addWordViewItems(mergedWord, partsOfSpeechFilter, items, wordFrequencyLevelAndRatio)
                items.add(WordDividerViewItem())
            }
    }

    private fun addWordsGroupedBySource(
        words: List<WordTeacherWord>,
        partsOfSpeechFilter: List<WordTeacherWord.PartOfSpeech>,
        items: MutableList<BaseViewItem<*>>,
        wordFrequencyLevelAndRatio: WordFrequencyLevelAndRatio?,
    ) {
        words.onEachIndexed { i, word ->
            val isAdded = addWordViewItems(word, partsOfSpeechFilter, items, if (i == 0) wordFrequencyLevelAndRatio else null)
            if (isAdded) {
                items.add(WordDividerViewItem())
            }
        }
    }

    private fun addWordViewItems(
        word: WordTeacherWord,
        partsOfSpeechFilter: List<WordTeacherWord.PartOfSpeech>,
        items: MutableList<BaseViewItem<*>>,
        wordFrequencyLevelAndRatio: WordFrequencyLevelAndRatio?,
    ): Boolean {
        val topIndex = items.size
        for (partOfSpeech in word.definitions.keys.filter {
            partsOfSpeechFilter.isEmpty() || partsOfSpeechFilter.contains(it)
        }) {
            items.add(WordPartOfSpeechViewItem(partOfSpeech.toStringDesc(), partOfSpeech))

            for (def in word.definitions[partOfSpeech].orEmpty()) {
                var isFirstDef = true
                for (d in def.definitions) {
                    items.add(
                        WordDefinitionViewItem(
                            definition = d,
                            withAddButton = isFirstDef,
                            data = WordDefinitionViewData(
                                word = word,
                                partOfSpeech = partOfSpeech,
                                def = def
                            ),
                            labels = if (isFirstDef) def.labels.orEmpty() else emptyList()
                        )
                    )
                    isFirstDef = false
                }

                if (def.examples?.isNotEmpty() == true) {
                    items.add(WordSubHeaderViewItem(
                        StringDesc.Resource(MR.strings.word_section_examples),
                        Indent.SMALL
                    ))
                    for (ex in def.examples.orEmpty()) {
                        items.add(WordExampleViewItem(ex, Indent.SMALL))
                    }
                }

                if (def.synonyms?.isNotEmpty() == true) {
                    items.add(WordSubHeaderViewItem(
                        StringDesc.Resource(MR.strings.word_section_synonyms),
                        Indent.SMALL
                    ))
                    for (synonym in def.synonyms) {
                        items.add(WordSynonymViewItem(synonym, Indent.SMALL))
                    }
                }
            }
        }

        val hasNewItems = items.size - topIndex > 0
        if (hasNewItems) {
            items.add(topIndex, WordTitleViewItem(word.word, word.types, frequencyLevelAndRatio = wordFrequencyLevelAndRatio))
            if (word.transcriptions?.isNotEmpty() == true) {
                items.add(topIndex + 1, WordTranscriptionViewItem(word.transcriptions.joinToString(", ")))
            }
        }

        return hasNewItems
    }

    private fun mergeWords(
        words: List<WordTeacherWord>,
        partsOfSpeechFilter: List<WordTeacherWord.PartOfSpeech>
    ): WordTeacherWord {
        if (words.size == 1) return words.first()

        val allWords = mutableListOf<String>()
        val allTranscriptions = mutableListOf<String>()
        val allDefinitions = LinkedHashMap<WordTeacherWord.PartOfSpeech, List<WordTeacherDefinition>>()
        val allTypes = mutableListOf<Config.Type>()

        words.forEach {
            if (!allWords.contains(it.word)) {
                allWords.add(it.word)
            }

            it.transcriptions?.onEach {
                if (!allTranscriptions.contains(it)) {
                    allTranscriptions.add(it)
                }
            }

            for (partOfSpeech in it.definitions.keys.filter { definition ->
                partsOfSpeechFilter.isEmpty() || partsOfSpeechFilter.contains(definition)
            }) {
                val originalDefs = it.definitions[partOfSpeech] as? MutableList ?: continue
                var list = allDefinitions[partOfSpeech] as? MutableList
                if (list == null) {
                    list = mutableListOf()
                    allDefinitions[partOfSpeech] = list
                }

                list.addAll(originalDefs)
            }

            for (type in it.types) {
                if (!allTypes.contains(type)) {
                    allTypes.add(type)
                }
            }
        }

        val resultWord = allWords.joinToString()
        val resultTranscription = if (allTranscriptions.isEmpty())
                emptyList<String>()
            else
                allTranscriptions

        return WordTeacherWord(resultWord, resultTranscription, allDefinitions, allTypes)
    }

    override fun getErrorText(res: Resource<*>): StringDesc? {
        val hasConnection = connectivityManager.isDeviceOnline
        val hasResponse = true // TODO: handle error server response
        return res.getErrorString(hasConnection, hasResponse)
    }

    override fun onEventHandled(event: DefinitionsVM.Event, withAction: Boolean) {
        when (event) {
            is DefinitionsVM.Event.CardSetUpdatedEvent -> onCardSetUpdatedEvent(event, withAction)
        }
    }

    private fun onCardSetUpdatedEvent(
        event: DefinitionsVM.Event.CardSetUpdatedEvent,
        needOpen: Boolean,
    ) {
        events.update {
            it.copy(cardSetUpdatedEvents = it.cardSetUpdatedEvents.filter { it != event })
        }
        if (needOpen) {
            router?.openCardSet(CardSetVM.State.LocalCardSet(event.id))
        }
    }

    // card sets
    override val cardSets = combine(cardSetsRepository.cardSets, settings.getBooleanFlow(SETTING_EXPAND_CARDSETS_POPUP, false)) { cardsets, isExpanded ->
        //Logger.v("build view items")
        cardsets.copyWith(buildCardSetViewItems(cardsets.data().orEmpty(), isExpanded))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    private fun buildCardSetViewItems(cardSets: List<ShortCardSet>, isExpanded: Boolean): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()

        var sortedCardSets = cardSets.sortedByDescending { it.modificationDate }
        var needExpandViewItem = false
        var needCollapseViewItem = false
        if (sortedCardSets.size > TOP_CARDSETS_IN_POUPUP_COUNT) {
            if (isExpanded) {
                needCollapseViewItem = true
            } else {
                needExpandViewItem = true
                sortedCardSets = sortedCardSets.take(TOP_CARDSETS_IN_POUPUP_COUNT)
            }
        }

        sortedCardSets.forEach {
            items.add(CardSetViewItem(it.id, it.name, ""))
        }
        if (needExpandViewItem) {
            items += CardSetExpandOrCollapseViewItem(
                isExpanded = false,
                text = StringDesc.Resource(MR.strings.definitions_cardsets_expand)
            )
        } else if (needCollapseViewItem) {
            items += CardSetExpandOrCollapseViewItem(
                isExpanded = true,
                text = StringDesc.Resource(MR.strings.definitions_cardsets_collapse)
            )
        }

        return listOf(
            *items.toTypedArray(),
            OpenCardSetViewItem(
                text = StringDesc.Resource(MR.strings.definitions_open_cardsets)
            )
        )
    }

    override fun onOpenCardSets(item: OpenCardSetViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("Definitions.openCardSets"))
        router?.openCardSets()
    }

    override fun onAddDefinitionInSet(
        wordDefinitionViewItem: WordDefinitionViewItem,
        cardSetViewItem: CardSetViewItem
    ) {
        analytics.send(AnalyticEvent.createActionEvent("Definitions.addDefinitionInSet"))
        val viewData = wordDefinitionViewItem.data as WordDefinitionViewData
        val contextExamples = definitionsContext?.wordContexts?.get(viewData.partOfSpeech)?.examples ?:
        definitionsContext?.wordContexts?.values?.map { it.examples }?.flatten() ?: emptyList()

        viewModelScope.launch {
            cardSetsRepository.addCard(
                setId = cardSetViewItem.cardSetId,
                term = viewData.word.word,
                definitions = viewData.def.definitions,
                labels = viewData.def.labels.orEmpty(),
                partOfSpeech = viewData.partOfSpeech,
                transcription = viewData.word.transcriptions?.firstOrNull(),
                synonyms = viewData.def.synonyms.orEmpty(),
                examples = viewData.def.examples.orEmpty() + contextExamples,
                termFrequency = wordFrequency.value.data()
            )
            events.update {
                it.copy(
                    cardSetUpdatedEvents = it.cardSetUpdatedEvents +
                        DefinitionsVM.Event.CardSetUpdatedEvent(
                            text = StringDesc.Resource(MR.strings.definitions_cardsets_card_added),
                            openText = StringDesc.Resource(MR.strings.definitions_cardsets_open),
                            id = cardSetViewItem.cardSetId
                        )
                )
            }
        }
    }

    override fun onCardSetExpandCollapseClicked(item: CardSetExpandOrCollapseViewItem) {
        viewModelScope.launch {
            settings.putBoolean(SETTING_EXPAND_CARDSETS_POPUP, !item.isExpanded)
        }
    }

    // suggests
    override val suggests = combine(suggestedDictEntryRepository.stateFlow, wordTextSearchRepository.stateFlow) { dictEntries, wordTextSearch ->
        dictEntries.merge(if (wordTextSearch.isUninitialized()){
            wordTextSearch.toLoaded(emptyList()) // treat unitialized as loaded not to get unitialized during merge
        } else {
            wordTextSearch
        }) { dictEntryList, dictTextSearchList ->
            val viewItems = dictEntryList.orEmpty().map {
                WordSuggestDictEntryViewItem(
                    word = it.word,
                    definition = "", // TODO: support first definition
                    source = it.dict.name
                )
            }.distinctBy { it.firstItem() } + // here we loose source to avoid duplications
            if (wordTextSearch.isLoading()) {
                listOf(WordLoadingViewItem())
            } else {
                dictTextSearchList.orEmpty().mapIndexed { wordIndex, word ->
                    word.defPairs.mapIndexed { defPairIndex, defPair ->
                        defPair.defEntries.mapIndexed { defEntryIndex, defEntry ->
                            defEntry.examples.orEmpty()
                                .mapIndexed { exampleIndex, example ->
                                    WordSuggestByTextViewItem(
                                        foundText = example,
                                        wordIndex = wordIndex,
                                        defPairIndex = defPairIndex,
                                        defEntryIndex = defEntryIndex,
                                        exampleIndex = exampleIndex,
                                        source = ""
                                    ) as BaseViewItem<*>
                                }
                        }.flatten()
                    }.flatten()
                }.flatten()
                .let {
                    if (it.isNotEmpty()) {
                        listOf(
                            WordTextSearchHeaderViewItem(
                                StringDesc.Resource(MR.strings.definitions_textsearch_title),
                                StringDesc.Resource(MR.strings.definitions_textsearch_showAllWords),
                                isTop = dictEntryList.orEmpty().isEmpty(),
                            )
                        ) + it
                    } else {
                        it
                    }
                }
            }
            viewItems.onEachIndexed { index, item -> item.id = index.toLong() }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    override fun clearSuggests() {
        suggestedDictEntryRepository.clear()
        wordTextSearchRepository.clear()
    }

    private var suggestJob: Job? = null
    override fun requestSuggests(word: String) {
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch(Dispatchers.Default) {
            delay(100)
            suggestedDictEntryRepository.load(word)
                .waitUntilDone {
                    if (it.size in 0..19) {
                        launch {
                            wordTextSearchRepository.load(word).collect()
                        }
                    }
                }
        }
    }

    override fun onSuggestedSearchWordClicked(item: WordSuggestByTextViewItem) {
        wordTextSearchRepository.value.onData { textSearchItems ->
            textSearchItems.getOrNull(item.wordIndex)?.let { word ->
                definitionWords.update {
                    Resource.Loaded(listOf(word.toWordTeacherWord()))
                }
            }
        }
    }

    override fun onSuggestedShowAllSearchWordClicked() {
        wordTextSearchRepository.value.onData { textSearchItems ->
            definitionWords.update {
                Resource.Loaded(
                    textSearchItems.map { it.toWordTeacherWord() }
                )
            }
        }
    }

    // word history
    override val wordHistory: StateFlow<Resource<List<BaseViewItem<*>>>> = wordDefinitionHistoryRepository.stateFlow.map { res ->
        res.mapLoadedData { words ->
            words.mapIndexed { index, s ->
                WordHistoryViewItem(index.toLong(), s) as BaseViewItem<*>
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())
    override val isWordHistorySelected = MutableStateFlow<Boolean>(false)

    override fun toggleWordHistory() {
        isWordHistorySelected.update { !it }
    }

    override fun onWordHistoryClicked(item: WordHistoryViewItem) {
        loadIfNeeded(item.firstItem())
    }
}

private data class WordDefinitionViewData(
    val word: WordTeacherWord,
    val partOfSpeech: WordTeacherWord.PartOfSpeech,
    val def: WordTeacherDefinition
)

data class DefinitionsContext(
    val wordContexts: Map<WordTeacherWord.PartOfSpeech, DefinitionsWordContext>
)

data class DefinitionsWordContext(
    val examples: List<String>
)

private const val SETTING_EXPAND_CARDSETS_POPUP = "expandCardSetsPopup"
private const val SETTING_LAST_DEFINED_WORD = "lastDefinedWord"
private const val SETTING_DEFINITION_DISPLAY_MODE = "definitionDisplayMode"
private const val SETTING_DEFINITION_DISPLAY_MODE_BY_SOURCE = 0
private const val SETTING_DEFINITION_DISPLAY_MODE_COMBINED = 1
private const val TOP_CARDSETS_IN_POUPUP_COUNT = 5