package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.getErrorString
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.toStringDesc
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class DefinitionsVM(
    private val connectivityManager: ConnectivityManager,
    private val wordDefinitionRepository: WordDefinitionRepository,
    private val idGenerator: IdGenerator,
    val state: State
): ViewModel() {

    private val definitionWords = MutableStateFlow<Resource<List<WordTeacherWord>>>(Resource.Uninitialized())
    val definitions = MutableStateFlow<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())
    val displayModes = listOf(DefinitionsDisplayMode.BySource, DefinitionsDisplayMode.Merged)
    var loadJob: Job? = null

    // State
    var displayModeIndex: Int = 0
    var displayMode: DefinitionsDisplayMode
        set(value) {
            displayModeIndex = displayModes.indexOf(value)
        }
        get() {
            return displayModes[displayModeIndex]
        }
    var word: String?
        get() {
            return state.word
        }
        set(value) {
            state.word = value
        }

    init {
        viewModelScope.launch {
            definitionWords.map {
                Logger.v("build view items")
                it.copyWith(buildViewItems(it.data() ?: emptyList()))
            }.forward(definitions)
        }

        word?.let {
            loadIfNeeded(it)
        } ?: run {
            loadIfNeeded("owl")
        }
    }

    // Events

    fun onWordSubmitted(word: String) {
        if (word.isNotEmpty()) {
            loadIfNeeded(word)
        }
    }

    fun onDisplayModeChanged(mode: DefinitionsDisplayMode) {
        if (this.displayMode == mode) return

        val words = wordDefinitionRepository.obtainStateFlow(this.word!!).value
        if (words.isLoaded()) {
            this.displayMode = mode
            definitions.value = Resource.Loaded(buildViewItems(words.data()!!))
        }
    }

    fun onTryAgainClicked() {
        loadIfNeeded(word!!)
    }

    // Actions

    private fun loadIfNeeded(word: String) {
        val stateFlow = wordDefinitionRepository.obtainStateFlow(word)
        if (stateFlow.value.isLoaded()) {
            definitionWords.value = stateFlow.value
        } else {
            load(word)
        }
    }

    private fun load(word: String) {
        val tag = "DefinitionsVM.load"

        Logger.v("Start Loading $word", tag)
        this.word = word

        loadJob?.cancel()
        loadJob = viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            Logger.e("Load Word exception for $word ${e.message}", tag)
        }) {
            wordDefinitionRepository.define(word, false).forward(definitionWords)
            Logger.v("Finish Loading $word", tag)
        }
    }

    private fun buildViewItems(words: List<WordTeacherWord>): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()
        items.add(DefinitionsDisplayModeViewItem(displayModes, displayModeIndex))
        items.add(WordDividerViewItem())

        when (displayMode) {
            DefinitionsDisplayMode.Merged -> addMergedWords(words, items)
            else -> addWordsGroupedBySource(words, items)
        }

        generateIds(items)
        return items
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
                map.remove(itemsHashCode)
            } else {
                it.id = idGenerator.nextId()
            }
        }
    }

    private fun addMergedWords(words: List<WordTeacherWord>, items: MutableList<BaseViewItem<*>>) {
        val word = mergeWords(words)

        addWordViewItems(word, items)
        items.add(WordDividerViewItem())
    }

    private fun addWordsGroupedBySource(words: List<WordTeacherWord>, items: MutableList<BaseViewItem<*>>) {
        for (word in words) {
            addWordViewItems(word, items)
            items.add(WordDividerViewItem())
        }
    }

    private fun addWordViewItems(word: WordTeacherWord, items: MutableList<BaseViewItem<*>>) {
        items.add(WordTitleViewItem(word.word, word.types))
        word.transcription?.let {
            items.add(WordTranscriptionViewItem(it))
        }

        for (partOfSpeech in word.definitions.keys) {
            items.add(WordPartOfSpeechViewItem(partOfSpeech.toStringDesc()))

            for (def in word.definitions[partOfSpeech].orEmpty()) {
                for (d in def.definitions) {
                    items.add(WordDefinitionViewItem(d))
                }

                if (def.examples.isNotEmpty()) {
                    items.add(WordSubHeaderViewItem(StringDesc.Resource(MR.strings.word_section_examples)))
                    for (ex in def.examples) {
                        items.add(WordExampleViewItem(ex))
                    }
                }

                if (def.synonyms.isNotEmpty()) {
                    items.add(WordSubHeaderViewItem(StringDesc.Resource(MR.strings.word_section_synonyms)))
                    for (synonym in def.synonyms) {
                        items.add(WordSynonymViewItem(synonym))
                    }
                }
            }
        }
    }

    private fun mergeWords(words: List<WordTeacherWord>): WordTeacherWord {
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

            for (partOfSpeech in it.definitions) {
                val originalDefs = it.definitions[partOfSpeech.key] as? MutableList ?: continue
                var list = allDefinitions[partOfSpeech.key] as? MutableList
                if (list == null) {
                    list = mutableListOf()
                    allDefinitions[partOfSpeech.key] = list
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
        val resultDefinitions = allDefinitions.mapValues {
            listOf(mergeDefinitions(it.value))
        }

        return WordTeacherWord(resultWord, resultTranscription, resultDefinitions, allTypes)
    }

    private fun mergeDefinitions(defs: List<WordTeacherDefinition>): WordTeacherDefinition {
        val allDefs = mutableListOf<String>()
        val allExamples = mutableListOf<String>()
        val allSynonyms = mutableListOf<String>()

        defs.forEach {
            for (d in it.definitions) {
                if (!allDefs.contains(d)) {
                    allDefs.add(d)
                }
            }

            for (example in it.examples) {
                if (!allExamples.contains(example)) {
                    allExamples.add(example)
                }
            }

            for (synonym in it.synonyms) {
                if (!allSynonyms.contains(synonym)) {
                    allSynonyms.add(synonym)
                }
            }
        }

        return WordTeacherDefinition(allDefs, allExamples, allSynonyms, null)
    }

    fun getErrorText(res: Resource<*>): StringDesc? {
        val hasConnection = connectivityManager.isDeviceOnline
        val hasResponse = true // TODO: handle error server response
        return res.getErrorString(hasConnection, hasResponse)
    }

    @Parcelize
    class State(
        var word: String? = null
    ): Parcelable
}