package com.aglushkov.wordteacher.shared.repository.cardset

import com.aglushkov.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.ShortCardSet
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

class CardSetsRepository(
    private val database: AppDatabase
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<List<ShortCardSet>>>(Resource.Uninitialized())

    val cardSets: StateFlow<Resource<List<ShortCardSet>>> = stateFlow

    init {
        scope.launch(Dispatchers.Default) {
            database.cardSets.selectAll().asFlow().collect {
                val result = it.executeAsList()
                Logger.v("CardSetsRepository loaded ${result.size} card sets")
                stateFlow.value = Resource.Loaded(result)
            }
        }
    }

    suspend fun createCardSet(name: String, date: Long) = supervisorScope {
        // Async in the scope to avoid retaining the parent coroutine and to cancel immediately
        // when it cancels (when corresponding ViewModel is cleared for example)
        scope.async(Dispatchers.Default) {
            database.cardSets.insert(name, date)
        }.await()
    }

    suspend fun removeCardSet(cardSetId: Long) = supervisorScope {
        scope.async(Dispatchers.Default) {
            removeCardSetInternal(cardSetId)
        }.await()
    }

    private fun removeCardSetInternal(cardSetId: Long) {
        database.cardSets.run {
            removeCardSet(cardSetId)
        }
    }
}
