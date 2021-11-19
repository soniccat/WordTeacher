package com.aglushkov.wordteacher.shared.repository.cardset

import com.aglushkov.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class CardSetRepository(
    private val database: AppDatabase
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<CardSet>>(Resource.Uninitialized())

    val article: StateFlow<Resource<CardSet>> = stateFlow
    private var loadJob: Job? = null

    suspend fun loadArticle(id: Long) {
        loadJob?.cancel()
        loadJob = scope.launch(Dispatchers.Default) {
            database.cardSets.selectCardSet(id).asFlow().collect {
                val result = it.executeAsOneOrNull()
                stateFlow.value = if (result != null) {
                    Resource.Loaded(result)
                } else {
                    Resource.Error(ArticleNotFound(), true)
                }
            }
        }
    }
}

class ArticleNotFound: Throwable("Article not found")