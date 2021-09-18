package com.aglushkov.wordteacher.androidApp.features.definitions.views

import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayMode
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayModeViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

open class DefinitionsVMPreview(
    val defs: Resource<List<BaseViewItem<*>>>,
    val displayMode: DefinitionsDisplayMode = DefinitionsDisplayMode.BySource,
    val partsOfSpeechFilter: List<WordTeacherWord.PartOfSpeech> = emptyList(),
    val selectedPartsOfSpeech: List<WordTeacherWord.PartOfSpeech> = emptyList(),
    val events: List<Event> = emptyList()
) : DefinitionsVM {
    override fun restore(newState: DefinitionsVM.State) {
    }

    override fun onWordSubmitted(word: String?, filter: List<WordTeacherWord.PartOfSpeech>) {
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
}
