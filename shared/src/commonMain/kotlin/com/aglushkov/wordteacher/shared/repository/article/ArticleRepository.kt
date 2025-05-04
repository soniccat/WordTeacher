package com.aglushkov.wordteacher.shared.repository.article

import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.onData
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ArticleRepository(
    private val database: AppDatabase
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<Article>>(Resource.Uninitialized())

    val article: StateFlow<Resource<Article>> = stateFlow
    private var loadJob: Job? = null

    fun loadArticle(id: Long) {
        loadJob?.cancel()
        loadJob = scope.launch(Dispatchers.Default) {
            database.articles.selectArticle(id).collect { article ->
                stateFlow.value = if (article != null) {
                    Resource.Loaded(article)
                } else {
                    Resource.Error(ArticleNotFound(), true)
                }
            }
        }
    }

    fun markAsRead(isRead: Boolean) {
        scope.launch(Dispatchers.Default) {
            stateFlow.value.onData {
                database.articles.setIsRead(it.id, isRead)
            }
        }
    }

    fun cancel() {
        scope.cancel()
    }
}

class ArticleNotFound: Throwable("Article not found")