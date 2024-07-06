package com.aglushkov.wordteacher.shared.features.notes.vm

import com.aglushkov.wordteacher.shared.general.Clearable
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.Note
import com.aglushkov.wordteacher.shared.repository.note.NotesRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import dev.icerock.moko.resources.desc.Raw
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// TODO: remove this feature
interface NotesVM: Clearable {
    val notes: StateFlow<Resource<List<BaseViewItem<*>>>>
    val stateFlow: StateFlow<State>
    val editingStateFlow: StateFlow<EditingState>

    fun restore(newState: State)

    fun onNoteAdded(text: String)
    fun onNewNoteTextChange(text: String)

    fun onNoteRemoved(item: NoteViewItem)
    fun onNoteClicked(item: NoteViewItem)
    fun onEditingTextChanged(text: String)
    fun onEditingCompleted()

    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?
    fun onTryAgainClicked()

    @Parcelize
    data class State(
        val newNoteText: String? = null
    ): Parcelable

    data class EditingState(
        val item: NoteViewItem? = null
    )
}

open class NotesVMImpl(
    val notesRepository: NotesRepository,
    private val timeSource: TimeSource,
    var state: NotesVM.State // TODO: remove as we already have stateFlow
): ViewModel(), NotesVM {
    final override val stateFlow = MutableStateFlow(state)
    final override val editingStateFlow = MutableStateFlow(EmptyEditingState)

    override val notes = combine(notesRepository.notes, stateFlow) { a, b -> a to b }
    .map { (notes, state) ->
        //Logger.v("build view items")
        notes.copyWith(buildViewItems(notes.data() ?: emptyList(), state.newNoteText))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    override fun restore(newState: NotesVM.State) {
        updateState(newState)
    }

    private fun updateState(newState: NotesVM.State) {
        state = newState
        stateFlow.value = state
    }

    override fun onNewNoteTextChange(text: String) {
        updateState(state.copy(newNoteText = text))
    }

    override fun onNoteAdded(text: String) {
        updateState(state.copy(newNoteText = null))

        viewModelScope.launch {
            try {
                notesRepository.createNote(timeSource.timeInMilliseconds(), text)
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    override fun onNoteRemoved(item: NoteViewItem) {
        viewModelScope.launch {
            try {
                notesRepository.removeNote(item.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    override fun onNoteClicked(item: NoteViewItem) {
        if (editingStateFlow.value.item != item) {
            editingStateFlow.value = editingStateFlow.value.copy(
                item = item
            )
        }
    }

    override fun onEditingTextChanged(text: String) {
        editingStateFlow.value.item?.let { item ->
            val originalText = item.text

            editingStateFlow.value = editingStateFlow.value.copy(
                item = item.apply {
                    this.text = text
                }
            )

            viewModelScope.launch {
                try {
                    notesRepository.updateNote(item.id, text)
                } catch (e: Exception) {
                    showError(e)

                    // rollback the changes
                    editingStateFlow.value = editingStateFlow.value.copy(
                        item = item.apply {
                            this.text = originalText
                        }
                    )
                }
            }
        }
    }

    override fun onEditingCompleted() {
        editingStateFlow.value = EmptyEditingState
    }

    private fun showError(e: Exception) {
        val errorText = e.message?.let {
            StringDesc.Raw(it)
        } ?: StringDesc.Resource(MR.strings.error_default)

        // TODO: pass an error message
        //eventChannel.offer(ErrorEvent(errorText))
    }

    private fun buildViewItems(articles: List<Note>, newNoteText: String?): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()
        articles.forEach {
            items.add(NoteViewItem(it.id, timeSource.stringDate(it.date), it.text))
        }

        return listOf(
            CreateNoteViewItem(
                placeholder = StringDesc.Resource(MR.strings.notes_create_note),
                text = newNoteText.orEmpty()
            ),
            *items.toTypedArray())
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.notes_error)
    }

    override fun onTryAgainClicked() {
        // TODO: do sth with notesRepository
    }
}

private val EmptyEditingState = NotesVM.EditingState()