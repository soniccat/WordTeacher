package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.getErrorString
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.toStringDesc
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.decompose.statekeeper.Parcelable
import com.arkivanov.decompose.statekeeper.Parcelize
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface DefinitionsVM {
    fun restore(newState: State)
    fun onWordSubmitted(word: String?, filter: List<WordTeacherWord.PartOfSpeech> = emptyList())
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

    @Parcelize
    class State(
        var word: String? = null
    ): Parcelable
}

open class DefinitionsVMImpl(
    private val connectivityManager: ConnectivityManager,
    private val wordDefinitionRepository: WordDefinitionRepository,
    private val idGenerator: IdGenerator,
    override var state: DefinitionsVM.State
): ViewModel(), DefinitionsVM {

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()
    private val definitionWords = MutableStateFlow<Resource<List<WordTeacherWord>>>(Resource.Uninitialized())

    final override var displayModeStateFlow = MutableStateFlow(DefinitionsDisplayMode.BySource)
    final override var selectedPartsOfSpeechStateFlow = MutableStateFlow<List<WordTeacherWord.PartOfSpeech>>(emptyList())

    override val definitions = combine(definitionWords, displayModeStateFlow, selectedPartsOfSpeechStateFlow) { a, b, c -> Triple(a, b, c) }
        .map { (wordDefinitions, displayMode, partOfSpeechFilter) ->
            Logger.v("build view items")
            wordDefinitions.copyWith(
                buildViewItems(wordDefinitions.data().orEmpty(), displayMode, partOfSpeechFilter)
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    override val partsOfSpeechFilterStateFlow = definitionWords.map {
        it.data().orEmpty().map { word ->
            word.definitions.keys
        }.flatten().distinct()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val displayModes = listOf(DefinitionsDisplayMode.BySource, DefinitionsDisplayMode.Merged)
    private var loadJob: Job? = null

    private var word: String?
        get() {
            return state.word
        }
        set(value) {
            state.word = value
        }

    init {
        word?.let {
            loadIfNeeded(it)
        }
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

    override fun onWordSubmitted(word: String?, filter: List<WordTeacherWord.PartOfSpeech>) {
        selectedPartsOfSpeechStateFlow.value = filter
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
            eventChannel.offer(
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

        loadJob?.cancel()
        loadJob = viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            Logger.e("Load Word exception for $word ${e.message}", tag)
        }) {
            wordDefinitionRepository.define(word, false).forward(definitionWords)
            Logger.v("Finish Loading $word", tag)
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

        when (displayMode) {
            DefinitionsDisplayMode.Merged -> addMergedWords(words, partsOfSpeechFilter, items)
            else -> addWordsGroupedBySource(words, partsOfSpeechFilter, items)
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

    // Set unique id taking into account that for the same items id shouldn't change
    private fun generateIds(items: MutableList<BaseViewItem<*>>) {
        val prevItems = definitions.value.data() ?: emptyList()
        val map: MutableMap<Int, MutableList<BaseViewItem<*>>> = mutableMapOf()

        // Put items with ids in map
        prevItems.forEach {
            val itemsHashCode = it.itemsHashCode()

            // Obtain mutable list
            val listOfViewItems: MutableList<BaseViewItem<*>> = map[itemsHashCode]
                ?: mutableListOf<BaseViewItem<*>>().also { list ->
                    map[itemsHashCode] = list
                }

            listOfViewItems.add(it)
        }

        // set ids for item not in the map
        items.forEach {
            val itemsHashCode = it.itemsHashCode()
            val mapListOfViewItems = map[itemsHashCode]
            val item = mapListOfViewItems?.firstOrNull { listItem -> listItem.items == it.items }

            if (item != null) {
                it.id = item.id
                mapListOfViewItems.remove(item)
            } else {
                it.id = idGenerator.nextId()
            }
        }
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
            items.add(WordPartOfSpeechViewItem(partOfSpeech.toStringDesc()))

            for (def in word.definitions[partOfSpeech].orEmpty()) {
                //items.add(WordHeaderViewItem(StringDesc.Resource(MR.strings.word_section_definition)))
                for (d in def.definitions) {
                    items.add(WordDefinitionViewItem("• $d"))
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
            items.add(topIndex, WordTitleViewItem(word.word, word.types))
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
