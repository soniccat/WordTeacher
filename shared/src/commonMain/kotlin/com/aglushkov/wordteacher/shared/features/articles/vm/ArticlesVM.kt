package com.aglushkov.wordteacher.shared.features.articles.vm

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.ShortArticle
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ArticlesVM(
    articlesRepository: ArticlesRepository,
    private val timeSource: TimeSource,
    private val router: ArticlesRouter
): ViewModel() {

    private val articlesFlow = articlesRepository.shortArticles
    val articles = MutableStateFlow<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())

    init {
        viewModelScope.launch {
            articlesFlow.map {
                Logger.v("build view items")
                it.copyWith(buildViewItems(it.data() ?: emptyList()))
            }.forward(articles)
        }
    }

    fun onCreateTextArticleClicked() {
        router.openAddArticle()
    }

    fun onArticleClicked(item: ArticleViewItem) {
        router.openArticle(item.id)
    }

    private fun buildViewItems(articles: List<ShortArticle>): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()
        articles.forEach {
            items.add(ArticleViewItem(it.id, it.name, timeSource.stringDate(it.date)))
        }

        return items
    }
}