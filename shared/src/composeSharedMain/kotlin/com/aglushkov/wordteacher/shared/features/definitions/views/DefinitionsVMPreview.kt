package com.aglushkov.wordteacher.shared.features.definitions.views

import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow

open class DefinitionsVMPreview(
    val defs: Resource<List<BaseViewItem<*>>>,
    val displayMode: DefinitionsDisplayMode = DefinitionsDisplayMode.BySource,
    val partsOfSpeechFilter: List<WordTeacherWord.PartOfSpeech> = emptyList(),
    val selectedPartsOfSpeech: List<WordTeacherWord.PartOfSpeech> = emptyList(),
    val events: List<Event> = emptyList()
) : DefinitionsVM {

    override var router: DefinitionsRouter? = null

    override fun onWordSubmitted(
        word: String?,
        filter: List<WordTeacherWord.PartOfSpeech>,
        definitionsContext: DefinitionsContext?
    ) {
    }

    override fun onTryAgainClicked() {
    }

    override fun onPartOfSpeechFilterUpdated(filter: List<WordTeacherWord.PartOfSpeech>) {
    }

    override fun onPartOfSpeechFilterClicked(item: DefinitionsDisplayModeViewItem) {
    }

    override fun onPartOfSpeechFilterCloseClicked(item: DefinitionsDisplayModeViewItem) {
    }

    override fun onDisplayModeChanged(mode: DefinitionsDisplayMode) {
    }

    override fun getErrorText(res: Resource<*>): StringDesc? {
        return StringDesc.Raw("Error Text")
    }

    override val state = DefinitionsVM.State(null)
    override val definitions: MutableStateFlow<Resource<List<BaseViewItem<*>>>>
        get() = MutableStateFlow(defs)

    override val displayModeStateFlow: StateFlow<DefinitionsDisplayMode>
        get() = MutableStateFlow(displayMode)
    override val partsOfSpeechFilterStateFlow: StateFlow<List<WordTeacherWord.PartOfSpeech>>
        get() = MutableStateFlow(partsOfSpeechFilter)
    override val selectedPartsOfSpeechStateFlow: StateFlow<List<WordTeacherWord.PartOfSpeech>>
        get() = MutableStateFlow(selectedPartsOfSpeech)
    override val eventFlow: Flow<Event>
        get() = events.asFlow()

    // Card Sets

    override val cardSets: StateFlow<Resource<List<BaseViewItem<*>>>>
        get() = MutableStateFlow(Resource.Uninitialized())

    override fun onOpenCardSets(item: OpenCardSetViewItem) {
    }

    override fun onAddDefinitionInSet(
        wordDefinitionViewItem: WordDefinitionViewItem,
        cardSetViewItem: CardSetViewItem
    ) {
    }

    // Suggests

    override val suggests: StateFlow<Resource<List<BaseViewItem<*>>>>
        get() = MutableStateFlow(Resource.Uninitialized())

    override fun clearSuggests() {}

    override fun requestSuggests(word: String) {}

    override fun onCleared() {}
}
