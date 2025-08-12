package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictWord
import com.aglushkov.wordteacher.shared.dicts.Dict
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetExpandOrCollapseViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
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
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
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


import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

interface DefinitionsVM: Clearable {
    var router: DefinitionsRouter?

    fun onWordTextUpdated(newText: String)
    fun onWordSubmitted(
        word: String?,
        filter: List<WordTeacherWord.PartOfSpeech> = emptyList(),
        definitionsContext: DefinitionsContext? = null
    )
    fun onWordClicked(
        word: String,
        filter: List<WordTeacherWord.PartOfSpeech> = emptyList(),
        definitionsContext: DefinitionsContext? = null
    )
    fun onTryAgainClicked()
    fun onPartOfSpeechFilterUpdated(filter: List<WordTeacherWord.PartOfSpeech>)
    fun onPartOfSpeechFilterCloseClicked(item: DefinitionsDisplayModeViewItem)
    fun onDisplayModeChanged(mode: DefinitionsDisplayMode)
    fun getErrorText(res: Resource<*>): StringDesc?
    fun onSuggestsAppeared()
    fun onBackPressed(): Boolean
    fun onAudioFileClicked(audioFile: WordAudioFilesViewItem.AudioFile)
    fun onCloseClicked()

    val wordTextValue: StateFlow<String>
    val state: State
    val definitions: StateFlow<Resource<List<BaseViewItem<*>>>>
    val partsOfSpeechFilterStateFlow: StateFlow<List<WordTeacherWord.PartOfSpeech>>
    val selectedPartsOfSpeechStateFlow: StateFlow<List<WordTeacherWord.PartOfSpeech>>
    val wordStack: StateFlow<List<String>>

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
    fun onWordHistoryItemClicked(item: WordHistoryViewItem)

    @Serializable
    data class State(
        var word: String? = null,
    )

    data class Settings(
        val needStoreDefinedWordInSettings: Boolean = false,
        val needShowLastDefinedWord: Boolean = false,
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
    private val settings: SettingStore,
    private val wordDefinitionHistoryRepository: WordDefinitionHistoryRepository,
    private val audioService: AudioService,
): ViewModel(), DefinitionsVM {

    override var router: DefinitionsRouter? = null
    final override var state: DefinitionsVM.State = restoredState.copy(
        word = if (definitionsSettings.needShowLastDefinedWord) {
            settings.string(SETTING_LAST_DEFINED_WORD) ?: restoredState.word
        } else {
            restoredState.word
        }
    )
    override val wordTextValue = MutableStateFlow(state.word.orEmpty())
    private val definitionWords = MutableStateFlow<Resource<List<WordTeacherWord>>>(Resource.Uninitialized())
    private val wordFrequency = MutableStateFlow<Resource<Double>>(Resource.Uninitialized())

    final override var selectedPartsOfSpeechStateFlow = MutableStateFlow<List<WordTeacherWord.PartOfSpeech>>(emptyList())
    override val wordStack = MutableStateFlow<List<String>>(emptyList())

    override val definitions = combine(
        definitionWords,
        settings.intFlow(SETTING_DEFINITION_DISPLAY_MODE, SETTING_DEFINITION_DISPLAY_MODE_BY_SOURCE),
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
        dictRepository.wordsStartWith(word, 60)
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

    override val isWordHistorySelected = MutableStateFlow(false)

    init {
        state.word?.let {
            updateCurrentWord(it)
        }

        if (definitionsSettings.needStoreDefinedWordInSettings) {
            viewModelScope.launch {
                // observe loaded definitions to form history
                definitionWords.collect {
                    if (it.data()?.isNotEmpty() == true) {
                        word?.let { w ->
                            settings[SETTING_LAST_DEFINED_WORD] = w
                            wordDefinitionHistoryRepository.put(w)
                        }
                    }
                }
            }
        }
    }

    // Events
    override fun onWordTextUpdated(newText: String) {
        wordTextValue.update { newText }
        if (newText.trim().isEmpty()) {
            clearSuggests()
        } else {
            requestSuggests(newText)
        }
    }

    override fun onWordSubmitted(
        word: String?,
        filter: List<WordTeacherWord.PartOfSpeech>,
        definitionsContext: DefinitionsContext?
    ) {
        if (word == null) {
            this.word = null
            wordTextValue.update { "" }
            selectedPartsOfSpeechStateFlow.value = emptyList()
            this.definitionsContext = null
        } else {
            updateCurrentWord(word, filter, definitionsContext, clearStack = true)
        }
    }

    override fun onWordClicked(
        word: String,
        filter: List<WordTeacherWord.PartOfSpeech>,
        definitionsContext: DefinitionsContext?
    ) {
        updateCurrentWord(word, filter, definitionsContext)
    }

    private fun updateCurrentWord(
        word: String,
        filter: List<WordTeacherWord.PartOfSpeech> = emptyList(),
        definitionsContext: DefinitionsContext? = null,
        putInWordStack: Boolean = true,
        clearStack: Boolean = false,
    ) {
        wordTextValue.update { word }
        selectedPartsOfSpeechStateFlow.value = filter
        this.definitionsContext = definitionsContext
        loadIfNeeded(word)
        if (putInWordStack) {
            if (clearStack) {
                wordStack.update { listOf(word) }
            } else {
                wordStack.update { it + word }
            }
        }
    }

    override fun onPartOfSpeechFilterUpdated(filter: List<WordTeacherWord.PartOfSpeech>) {
        analytics.send(AnalyticEvent.createActionEvent("Definitions.partOfSpeechFilterUpdated"))
        selectedPartsOfSpeechStateFlow.value = filter
    }

    override fun onPartOfSpeechFilterCloseClicked(item: DefinitionsDisplayModeViewItem) {
        selectedPartsOfSpeechStateFlow.value = emptyList()
    }

    override fun onDisplayModeChanged(mode: DefinitionsDisplayMode) {
        analytics.send(AnalyticEvent.createActionEvent("Definitions.displayModeChanged"))
        settings[SETTING_DEFINITION_DISPLAY_MODE] = mode.ordinal
    }

    override fun onTryAgainClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Definitions.onTryAgainClicked"))
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

        viewModelScope.launch(Dispatchers.IO) {
            loadResource {
                wordFrequencyGradationProvider.resolveFrequencyForWord(word)
            }.collect(wordFrequency)
        }

        wordRes.onLoaded(
            block = {
                val flattenedValue = it.map { it.second }.flatten()
                definitionWords.update {
                    it.toLoaded(flattenedValue).bumpVersion()
                }
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
        var isFirst = true
        words.groupBy { it.word }
            .map {
                val mergedWord = mergeWords(it.value, partsOfSpeechFilter)
                addWordViewItems(
                    mergedWord,
                    partsOfSpeechFilter,
                    items,
                    if (isFirst) wordFrequencyLevelAndRatio else null
                )
                items.add(WordDividerViewItem())
                isFirst = false
            }
    }

    private fun addWordsGroupedBySource(
        words: List<WordTeacherWord>,
        partsOfSpeechFilter: List<WordTeacherWord.PartOfSpeech>,
        items: MutableList<BaseViewItem<*>>,
        wordFrequencyLevelAndRatio: WordFrequencyLevelAndRatio?,
    ) {
        var isFirst = true
        words.onEachIndexed { i, word ->
            val isAdded = addWordViewItems(
                word,
                partsOfSpeechFilter,
                items,
                if (isFirst) wordFrequencyLevelAndRatio else null
            )
            if (isAdded) {
                items.add(WordDividerViewItem())
            }
            isFirst = false
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
                    def.examples.onEachIndexed { index, ex ->
                        items.add(WordExampleViewItem(ex, Indent.SMALL, isLast = index == def.examples.lastIndex))
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
            var insertIndex = topIndex + 1
            if (word.transcriptions?.isNotEmpty() == true) {
                items.add(insertIndex, WordTranscriptionViewItem(word.transcriptions.joinToString(", ")))
                insertIndex += 1
            }
            if (word.audioFiles.isNotEmpty()) {
                items.add(insertIndex, WordAudioFilesViewItem(word.audioFiles.map { audioFile ->
                    audioFile.toViewItemAudioFile()
                }))
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
        val allAudioFiles = mutableListOf<WordTeacherWord.AudioFile>()
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

            it.audioFiles.onEach {
                if (!allAudioFiles.contains(it)) {
                    allAudioFiles.add(it)
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

        return WordTeacherWord(
            allWords.joinToString(),
            allTranscriptions,
            allDefinitions,
            allTypes,
            allAudioFiles)
    }

    override fun getErrorText(res: Resource<*>): StringDesc? {
        val hasConnection = connectivityManager.isDeviceOnline
        val hasResponse = true // TODO: handle error server response
        return res.getErrorString(hasConnection, hasResponse)
    }

    override fun onSuggestsAppeared() {
        if (wordTextValue.value.isNotEmpty()) {
            requestSuggests(wordTextValue.value)
        }
    }

    override fun onBackPressed(): Boolean {
        if (wordStack.value.size > 1) {
            wordStack.update { it.take(it.size - 1) }
            updateCurrentWord(wordStack.value.last(), putInWordStack = false)
        }

        return false
    }

    // card sets
    override val cardSets = combine(cardSetsRepository.cardSets, settings.booleanFlow(SETTING_EXPAND_CARDSETS_POPUP, false)) { cardsets, isExpanded ->
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
                transcriptions = viewData.word.transcriptions,
                synonyms = viewData.def.synonyms.orEmpty(),
                examples = viewData.def.examples.orEmpty() + contextExamples,
                termFrequency = wordFrequency.value.data(),
                audioFiles = viewData.word.audioFiles,
            )
            router?.onLocalCardSetUpdated(cardSetViewItem.cardSetId)
        }
    }

    override fun onAudioFileClicked(audioFile: WordAudioFilesViewItem.AudioFile) {
        analytics.send(AnalyticEvent.createActionEvent("Definitions.onAudioFileClicked"))
        audioService.play(audioFile.url)
    }

    override fun onCardSetExpandCollapseClicked(item: CardSetExpandOrCollapseViewItem) {
        analytics.send(
            AnalyticEvent.createActionEvent(
                if (item.isExpanded) {
                    "Definitions.onCardSetExpandCollapsed"
                } else {
                    "Definitions.onCardSetExpandExpanded"
                }
            )
        )
        settings[SETTING_EXPAND_CARDSETS_POPUP] = !item.isExpanded
    }

    // suggests
    override val suggests = combine(
        suggestedDictEntryRepository.stateFlow,
        wordTextSearchRepository.stateFlow,
    ) { dictEntries, wordTextSearch ->
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
        suggestJob = null

        suggestJob = viewModelScope.launch(Dispatchers.IO) {
            delay(200)
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
        analytics.send(AnalyticEvent.createActionEvent("Definitions.suggestedSearchWordClicked"))
        wordTextSearchRepository.value.onData { textSearchItems ->
            textSearchItems.getOrNull(item.wordIndex)?.let { word ->
                definitionWords.update {
                    Resource.Loaded(listOf(word.toWordTeacherWord()))
                }
            }
        }
    }

    override fun onSuggestedShowAllSearchWordClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Definitions.suggestedShowAllSearchWordClicked"))
        wordTextSearchRepository.value.onData { textSearchItems ->
            wordFrequency.update { Resource.Uninitialized() }
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

    override fun toggleWordHistory() {
        analytics.send(
            AnalyticEvent.createActionEvent(
                if (isWordHistorySelected.value) {
                    "Definitions.hideWordHistory"
                } else {
                    "Definitions.showWordHistory"
                }
            )
        )
        isWordHistorySelected.update { !it }
    }

    override fun onWordHistoryItemClicked(item: WordHistoryViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("Definitions.wordHistoryItemClicked"))
        updateCurrentWord(item.firstItem(), clearStack = true)
    }

    override fun onCloseClicked() {
        router?.onDefinitionsClosed()
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

fun WordTeacherWord.AudioFile.toViewItemAudioFile(): WordAudioFilesViewItem.AudioFile {
    val name = accent ?: "Audio"
    var details = ""
    if (text != null) {
        details += text
    }
    if (transcription != null) {
        if (details.isNotEmpty()) {
            details += ", "
        }
        details += transcription
    }
    if (details.isNotEmpty()) {
        details = " ($details)"
    }
    return WordAudioFilesViewItem.AudioFile(
        url = url,
        name = name + details,
    )
}

private const val SETTING_EXPAND_CARDSETS_POPUP = "expandCardSetsPopup"
private const val SETTING_LAST_DEFINED_WORD = "lastDefinedWord"
private const val SETTING_DEFINITION_DISPLAY_MODE = "definitionDisplayMode"
private const val SETTING_DEFINITION_DISPLAY_MODE_BY_SOURCE = 0
private const val SETTING_DEFINITION_DISPLAY_MODE_COMBINED = 1
private const val TOP_CARDSETS_IN_POUPUP_COUNT = 5