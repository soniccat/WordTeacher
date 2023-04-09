package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.dicts.Dict
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.getErrorString
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.model.ShortCardSet
import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.model.nlp.NLPSpan
import com.aglushkov.wordteacher.shared.model.toStringDesc
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

interface DefinitionsVM: Clearable {
    var router: DefinitionsRouter?

    fun restore(newState: State)
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

    val state: State
    val definitions: StateFlow<Resource<List<BaseViewItem<*>>>>
    val displayModeStateFlow: StateFlow<DefinitionsDisplayMode>
    val partsOfSpeechFilterStateFlow: StateFlow<List<WordTeacherWord.PartOfSpeech>>
    val selectedPartsOfSpeechStateFlow: StateFlow<List<WordTeacherWord.PartOfSpeech>>
    val eventFlow: Flow<Event>

    // Card Sets
    val cardSets: StateFlow<Resource<List<BaseViewItem<*>>>>

    fun onOpenCardSets(item: OpenCardSetViewItem)
    fun onAddDefinitionInSet(wordDefinitionViewItem: WordDefinitionViewItem, cardSetViewItem: CardSetViewItem)

    // Suggests
    val suggests: StateFlow<Resource<List<BaseViewItem<*>>>>

    fun clearSuggests()
    fun requestSuggests(word: String)

    @Parcelize
    class State(
        var word: String? = null
    ): Parcelable
}

open class DefinitionsVMImpl(
    override var state: DefinitionsVM.State,
    private val connectivityManager: ConnectivityManager,
    private val wordDefinitionRepository: WordDefinitionRepository,
    private val dictRepository: DictRepository,
    private val cardSetsRepository: CardSetsRepository,
    private val idGenerator: IdGenerator,
): ViewModel(), DefinitionsVM {

    override var router: DefinitionsRouter? = null

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()
    private val definitionWords = MutableStateFlow<Resource<List<WordTeacherWord>>>(Resource.Uninitialized())

    final override var displayModeStateFlow = MutableStateFlow(DefinitionsDisplayMode.BySource)
    final override var selectedPartsOfSpeechStateFlow = MutableStateFlow<List<WordTeacherWord.PartOfSpeech>>(emptyList())

    private val wordDefinitionViewDataMap = mutableMapOf<String, WordDefinitionViewData>()

    override val definitions = combine(
        definitionWords,
        displayModeStateFlow,
        selectedPartsOfSpeechStateFlow
    ) { a, b, c -> Triple(a, b, c) }
    .map { (wordDefinitions, displayMode, partOfSpeechFilter) ->
        //Logger.v("build view items ${wordDefinitions.data()?.size ?: 0}")
        wordDefinitions.copyWith(
            buildViewItems(wordDefinitions.data().orEmpty(), displayMode, partOfSpeechFilter)
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    override val partsOfSpeechFilterStateFlow = definitionWords.map {
        it.data().orEmpty().map { word ->
            word.definitions.keys
        }.flatten().distinct()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val displayModes: ImmutableList<DefinitionsDisplayMode> = persistentListOf(DefinitionsDisplayMode.BySource, DefinitionsDisplayMode.Merged)
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

    override fun restore(newState: DefinitionsVM.State) {
        state = newState
        word?.let {
            loadIfNeeded(it)
        } ?: run {
            word = "owl"
            loadIfNeeded("owl")
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventChannel.cancel()
    }

    // Events

    override fun onWordSubmitted(
        word: String?,
        filter: List<WordTeacherWord.PartOfSpeech>,
        definitionsContext: DefinitionsContext?
    ) {
        selectedPartsOfSpeechStateFlow.value = filter
        this.definitionsContext = definitionsContext

        if (word == null) {
            this.word = null
        } else if (word.isNotEmpty()) {
            loadIfNeeded(word)
        }
    }

    override fun onPartOfSpeechFilterUpdated(filter: List<WordTeacherWord.PartOfSpeech>) {
        selectedPartsOfSpeechStateFlow.value = filter
    }

    override fun onPartOfSpeechFilterClicked(item: DefinitionsDisplayModeViewItem) {
        viewModelScope.launch {
            eventChannel.trySend(
                ShowPartsOfSpeechFilterDialogEvent(
                    selectedPartsOfSpeechStateFlow.value,
                    partsOfSpeechFilterStateFlow.value
                )
            )
        }
    }

    override fun onPartOfSpeechFilterCloseClicked(item: DefinitionsDisplayModeViewItem) {
        selectedPartsOfSpeechStateFlow.value = emptyList()
    }

    override fun onDisplayModeChanged(mode: DefinitionsDisplayMode) {
        if (displayModeStateFlow.value == mode) return
        displayModeStateFlow.value = mode
    }

    override fun onTryAgainClicked() {
        loadIfNeeded(word!!)
    }

    // Actions

    private fun loadIfNeeded(word: String) {
        this.word = word

        val stateFlow = wordDefinitionRepository.obtainStateFlow(word)
        if (stateFlow.value.isLoaded()) {
            definitionWords.value = if (definitionWords.value != stateFlow.value) {
                stateFlow.value
            } else {
                // HACK: copy to trigger flow event
                stateFlow.value.copy()
            }
        } else {
            load(word)
        }
    }

    private fun load(word: String) {
        val tag = "DefinitionsVM.load"
        Logger.v("Start Loading $word", tag)

        observeJob?.cancel()
        loadJob?.cancel()

        loadJob = viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            Logger.e("Load Word exception for $word ${e.message}", tag)
        }) {
            wordDefinitionRepository.define(word, false).forward(definitionWords)
            Logger.v("Finish Loading $word", tag)
        }

        observeJob = viewModelScope.launch {
            wordDefinitionRepository.obtainStateFlow(word).collect {
                if (it.isUninitialized()) {
                    load(word) // reload when cache is unloaded
                }
            }
        }
    }

    private fun buildViewItems(
        words: List<WordTeacherWord>,
        displayMode: DefinitionsDisplayMode,
        partsOfSpeechFilter: List<WordTeacherWord.PartOfSpeech>
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

        wordDefinitionViewDataMap.clear()
        when (displayMode) {
            DefinitionsDisplayMode.Merged -> addMergedWords(words, partsOfSpeechFilter, items)
            else -> addWordsGroupedBySource(words, partsOfSpeechFilter, items)
        }

        val itemsWithIds = generateIds(items)
        return itemsWithIds
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

    private fun generateIds(items: MutableList<BaseViewItem<*>>): List<BaseViewItem<*>> {
        return generateViewItemIds(items, definitions.value.data().orEmpty(), idGenerator)
    }

    private fun addMergedWords(
        words: List<WordTeacherWord>,
        partsOfSpeechFilter: List<WordTeacherWord.PartOfSpeech>,
        items: MutableList<BaseViewItem<*>>
    ) {
        val word = mergeWords(words, partsOfSpeechFilter)

        addWordViewItems(word, partsOfSpeechFilter, items)
        items.add(WordDividerViewItem())
    }

    private fun addWordsGroupedBySource(
        words: List<WordTeacherWord>,
        partsOfSpeechFilter: List<WordTeacherWord.PartOfSpeech>,
        items: MutableList<BaseViewItem<*>>
    ) {
        for (word in words) {
            val isAdded = addWordViewItems(word, partsOfSpeechFilter, items)
            if (isAdded) {
                items.add(WordDividerViewItem())
            }
        }
    }

    private fun addWordViewItems(
        word: WordTeacherWord,
        partsOfSpeechFilter: List<WordTeacherWord.PartOfSpeech>,
        items: MutableList<BaseViewItem<*>>
    ): Boolean {
        val topIndex = items.size
        for (partOfSpeech in word.definitions.keys.filter {
            partsOfSpeechFilter.isEmpty() || partsOfSpeechFilter.contains(it)
        }) {
            items.add(WordPartOfSpeechViewItem(partOfSpeech.toStringDesc(), partOfSpeech))

            for (def in word.definitions[partOfSpeech].orEmpty()) {
                for (d in def.definitions) {
                    val key = word.word + partOfSpeech.name + def.hashCode().toString()
                    wordDefinitionViewDataMap[key] = WordDefinitionViewData(
                        word = word,
                        partOfSpeech = partOfSpeech,
                        def = def
                    )
                    items.add(
                        WordDefinitionViewItem(
                            definition = d,
                            dataKey = key
                        )
                    )
                }

                if (def.examples.isNotEmpty()) {
                    items.add(WordSubHeaderViewItem(
                        StringDesc.Resource(MR.strings.word_section_examples),
                        Indent.SMALL
                    ))
                    for (ex in def.examples) {
                        items.add(WordExampleViewItem(ex, Indent.SMALL))
                    }
                }

                if (def.synonyms.isNotEmpty()) {
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
            items.add(topIndex, WordTitleViewItem(word.word, word.types.toImmutableList()))
            word.transcription?.let {
                items.add(topIndex + 1, WordTranscriptionViewItem(it))
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
        val allDefinitions = mutableMapOf<WordTeacherWord.PartOfSpeech, List<WordTeacherDefinition>>()
        val allTypes = mutableListOf<Config.Type>()

        words.forEach {
            if (!allWords.contains(it.word)) {
                allWords.add(it.word)
            }

            it.transcription?.let {
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
                null
            else
                allTranscriptions.joinToString()

        return WordTeacherWord(resultWord, resultTranscription, allDefinitions, allTypes)
    }

    override fun getErrorText(res: Resource<*>): StringDesc? {
        val hasConnection = connectivityManager.isDeviceOnline
        val hasResponse = true // TODO: handle error server response
        return res.getErrorString(hasConnection, hasResponse)
    }

    // card sets

    override val cardSets = cardSetsRepository.cardSets.map {
        //Logger.v("build view items")
        it.copyWith(buildCardSetViewItems(it.data() ?: emptyList()))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    private fun buildCardSetViewItems(cardSets: List<ShortCardSet>): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()

        cardSets.forEach {
            items.add(CardSetViewItem(it.id, it.name, ""))
        }

        return listOf(
            *items.toTypedArray(),
            OpenCardSetViewItem(
                text = StringDesc.Resource(MR.strings.definitions_open_cardsets)
            )
        )
    }

    override fun onOpenCardSets(item: OpenCardSetViewItem) {
        router?.openCardSets()
    }

    override fun onAddDefinitionInSet(
        wordDefinitionViewItem: WordDefinitionViewItem,
        cardSetViewItem: CardSetViewItem
    ) {
        val viewData = wordDefinitionViewDataMap[wordDefinitionViewItem.dataKey] ?: return
        val contextExamples = definitionsContext?.wordContexts?.get(viewData.partOfSpeech)?.examples ?:
        definitionsContext?.wordContexts?.values?.map { it.examples }?.flatten() ?: emptyList()

        viewModelScope.launch {
            cardSetsRepository.addCard(
                setId = cardSetViewItem.id,
                term = viewData.word.word,
                definitions = viewData.def.definitions,
                partOfSpeech = viewData.partOfSpeech,
                transcription = viewData.word.transcription,
                synonyms = viewData.def.synonyms,
                examples = viewData.def.examples + contextExamples
            )
        }
    }

    // suggests
    override val suggests = MutableStateFlow<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())

    override fun clearSuggests() {
        suggests.value = Resource.Uninitialized()
    }

    override fun requestSuggests(word: String) {
        var i = 0L
        val entries = dictRepository.wordsStartWith(word, 20).map {
            WordSuggestViewItem(
                id = i++,
                word = it.word,
                definition = "", // TODO: support first definition
                source = it.dict.name
            )
        }
        suggests.value = Resource.Loaded(entries)
    }
}

data class ShowPartsOfSpeechFilterDialogEvent(
    val partsOfSpeech: List<WordTeacherWord.PartOfSpeech>,
    val selectedPartsOfSpeech: List<WordTeacherWord.PartOfSpeech>,
    override var isHandled: Boolean = false
): Event {
    override fun markAsHandled() {
        isHandled = true
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
