package com.aglushkov.wordteacher.shared.features.notes.vm

import com.aglushkov.wordteacher.shared.events.CompletionEvent
import com.aglushkov.wordteacher.shared.events.CompletionResult
import com.aglushkov.wordteacher.shared.events.ErrorEvent
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.getErrorString
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Note
import com.aglushkov.wordteacher.shared.model.ShortArticle
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.note.NotesRepository
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Raw
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface NotesVM {
    val notes: StateFlow<Resource<List<BaseViewItem<*>>>>

    fun onNoteAdded(text: String)
    fun onNoteRemoved(item: NoteViewItem)
    fun onNoteClicked(item: NoteViewItem)

    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?
    fun onTryAgainClicked()
}

open class NotesVMImpl(
    val notesRepository: NotesRepository,
    private val timeSource: TimeSource
): ViewModel(), NotesVM {

    override val notes = notesRepository.notes.map {
        Logger.v("build view items")
        it.copyWith(buildViewItems(it.data() ?: emptyList()))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    override fun onNoteAdded(text: String) {
        viewModelScope.launch {
            try {
                notesRepository.createNote(timeSource.getTimeInMilliseconds(), text)
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
    }

    private fun showError(e: Exception) {
        val errorText = e.message?.let {
            StringDesc.Raw(it)
        } ?: StringDesc.Resource(MR.strings.error_default)

        // TODO: pass an error message
        //eventChannel.offer(ErrorEvent(errorText))
    }

    private fun buildViewItems(articles: List<Note>): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()
        articles.forEach {
            items.add(NoteViewItem(it.id, timeSource.stringDate(it.date), it.text))
        }

        return listOf(CreateNoteViewItem(StringDesc.Resource(MR.strings.notes_create_note)), *items.toTypedArray())
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.notes_error)
    }

    override fun onTryAgainClicked() {
        // TODO: do sth with notesRepository
    }
}
