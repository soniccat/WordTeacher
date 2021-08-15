package com.aglushkov.wordteacher.shared.features.articles.vm

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.ShortArticle
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface ArticlesVM {
    val articles: StateFlow<Resource<List<BaseViewItem<*>>>>

    fun onCreateTextArticleClicked()
    fun onArticleClicked(item: ArticleViewItem)
}

open class ArticlesVMImpl(
    articlesRepository: ArticlesRepository,
    private val timeSource: TimeSource,
    private val router: ArticlesRouter
): ViewModel(), ArticlesVM {

    override val articles = articlesRepository.shortArticles.map {
        Logger.v("build view items")
        it.copyWith(buildViewItems(it.data() ?: emptyList()))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    override fun onCreateTextArticleClicked() {
        router.openAddArticle()
    }

    override fun onArticleClicked(item: ArticleViewItem) {
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