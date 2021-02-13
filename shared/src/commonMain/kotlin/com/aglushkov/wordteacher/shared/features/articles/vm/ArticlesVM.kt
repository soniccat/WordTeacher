package com.aglushkov.wordteacher.shared.features.articles.vm

import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ArticlesVM(
    private val articlesRepository: ArticleRepository,
    private val idGenerator: IdGenerator,
): ViewModel() {

    private val articlesFlow = articlesRepository.articles //MutableStateFlow<Resource<List<Article>>>(Resource.Uninitialized())
    val articles = MutableStateFlow<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())

    init {
        viewModelScope.launch {
            articlesFlow.map {
                Logger.v("build view items")
                it.copyWith(buildViewItems(it.data() ?: emptyList()))
            }.forward(articles)
        }
    }

    suspend fun onTextAdded(text: String) = coroutineScope {
//        val article = Article(
//            0,
//            "unknown",
//            Clock.System.now().toEpochMilliseconds(),
//            text
//        )
//
//        articlesRepository.putArticle(article)
    }

    private fun buildViewItems(articles: List<Article>): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()
        articles.forEach {
            items.add(ArticleViewItem(it.name, it.date).apply {
                id = idGenerator.nextId()
            })
        }

        return items
    }
}