package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.features.definitions.repository.WordRepository
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.load
import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.Config
import dev.icerock.moko.mvvm.livedata.LiveData
import dev.icerock.moko.mvvm.livedata.MutableLiveData
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class DefinitionsVM(
    private val connectivityManager: ConnectivityManager,
    private val wordRepository: WordRepository,
    private val state: State
): ViewModel() {

    private val innerDefinitions = MutableLiveData<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())
    val definitions: LiveData<Resource<List<BaseViewItem<*>>>> = innerDefinitions

    // State
    var displayMode = DefinitionsDisplayMode.BySource
    var word: String?
        get() {
            return state.word
        }
        set(value) {
            state.word = value
        }

    init {
        word?.let {
            load(it)
        } ?: run {
            load("owl")
        }
    }

    // Events

    fun onWordSubmitted(word: String) {
        this.word?.let {
            wordRepository.clear(it)
        }

        if (word.isNotEmpty()) {
            innerDefinitions.value = Resource.Uninitialized()
            load(word)
        }
    }

    fun onDisplayModeChanged(mode: DefinitionsDisplayMode) {
        if (this.displayMode == mode) return

        val words = wordRepository.obtainStateFlow(this.word!!).value
        if (words.isLoaded()) {
            this.displayMode = mode
            innerDefinitions.value = Resource.Loaded(buildViewItems(words.data()!!))
        }
    }

    fun onTryAgainClicked() {
        load(word!!)
    }

    // Actions

    private fun load(word: String) {
        this.word = word
        viewModelScope.launch {
            innerDefinitions.load(true) {
                // TODO: handle Loading to show intermediate results
                val words = wordRepository.define(word).first {
                    if (it is Resource.Error) {
                        throw it.throwable
                    }

                    it.isLoaded()
                }.data()!!
                buildViewItems(words)
            }
        }
    }

    private fun buildViewItems(words: List<WordTeacherWord>): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()
        items.add(DefinitionsDisplayModeViewItem(listOf(DefinitionsDisplayMode.BySource, DefinitionsDisplayMode.Merged), displayMode))
        items.add(WordDividerViewItem())

        when (displayMode) {
            DefinitionsDisplayMode.Merged -> addMergedWords(words, items)
            else -> addWordsGroupedBySource(words, items)
        }

        return items
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
            items.add(WordPartOfSpeechViewItem(partOfSpeech.toString(getApplication())))

            for (def in word.definitions[partOfSpeech].orEmpty()) {
                for (d in def.definitions) {
                    items.add(WordDefinitionViewItem(d))
                }

                if (def.examples.isNotEmpty()) {
                    items.add(WordSubHeaderViewItem(getString(R.string.word_section_examples)))
                    for (ex in def.examples) {
                        items.add(WordExampleViewItem(ex))
                    }
                }

                if (def.synonyms.isNotEmpty()) {
                    items.add(WordSubHeaderViewItem(getString(R.string.word_section_synonyms)))
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

    fun getErrorText(res: Resource<*>): String? {
        val hasConnection = connectivityManager.isDeviceOnline
        val hasResponse = true // TODO: handle error server response
        return res.getErrorString(getApplication(), hasConnection, hasResponse)
    }

    class State {
        var word: String? = null
    }
}