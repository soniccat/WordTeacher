package com.aglushkov.wordteacher.shared.repository.note

import com.aglushkov.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.model.Note
import com.aglushkov.wordteacher.shared.model.ShortArticle
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class NotesRepository(
    private val database: AppDatabase,
    private val nlpCore: NLPCore
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<List<Note>>>(Resource.Uninitialized())

    val notes: StateFlow<Resource<List<Note>>> = stateFlow

    init {
        scope.launch(Dispatchers.Default) {
            database.notes.selectAll().asFlow().collect {
                val result = it.executeAsList()
                Logger.v("NotesRepository loaded ${result.size} notes")
                stateFlow.value = Resource.Loaded(result)
            }
        }
    }

    suspend fun createNote(date: Long, text: String) = supervisorScope {
        // Async in the scope to avoid retaining the parent coroutine and to cancel immediately
        // when it cancels (when corresponding ViewModel is cleared for example)
        scope.async(Dispatchers.Default) {
            createNoteInternal(date, text)
        }.await()
    }

    suspend fun removeNote(noteId: Long) = supervisorScope {
        scope.async(Dispatchers.Default) {
            removeNoteInternal(noteId)
        }.await()
    }

    private suspend fun createNoteInternal(date: Long, text: String): Note {
        nlpCore.waitUntilInitialized()

        val noteId = database.notes.run {
            insert(date, text)
            insertedNoteId()
        } ?: 0L

        return Note(noteId, date, text)
    }

    private fun removeNoteInternal(noteId: Long) {
        database.notes.run {
            removeNote(noteId)
        }
    }

    suspend fun updateNote(noteId: Long, text: String) = supervisorScope {
        scope.async(Dispatchers.Default) {
            updateNoteInternal(noteId, text)
        }.await()
    }

    private fun updateNoteInternal(noteId: Long, text: String) {
        database.notes.run {
            updateNote(noteId, text)
        }
    }
}
