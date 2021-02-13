package com.aglushkov.wordteacher.shared.features.articles.vm

import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock

class ArticlesVM(
    private val articlesRepository: ArticleRepository,
    private val idGenerator: IdGenerator,
): ViewModel() {

    private val articlesFlow = MutableStateFlow<Resource<List<Article>>>(Resource.Uninitialized())
    val articles = MutableStateFlow<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())

    init {
    }

    suspend fun onTextAdded(text: String) = coroutineScope {
        val article = Article(
            0,
            "unknown",
            Clock.System.now().toEpochMilliseconds(),
            text
        )

        articlesRepository.putArticle(article)
    }
}