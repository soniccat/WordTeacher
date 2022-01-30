package com.aglushkov.wordteacher.shared.repository.article

import com.aglushkov.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ArticleRepository(
    private val database: AppDatabase
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<Article>>(Resource.Uninitialized())

    val article: StateFlow<Resource<Article>> = stateFlow
    private var loadJob: Job? = null

    suspend fun loadArticle(id: Long) {
        loadJob?.cancel()
        loadJob = scope.launch(Dispatchers.Default) {
            database.articles.selectArticle(id).asFlow().collect {
                val result = it.executeAsOneOrNull()
                stateFlow.value = if (result != null) {
                    Resource.Loaded(result)
                } else {
                    Resource.Error(ArticleNotFound(), true)
                }
            }
        }
    }

    fun cancel() {
        scope.cancel()
    }
}

class ArticleNotFound: Throwable("Article not found")