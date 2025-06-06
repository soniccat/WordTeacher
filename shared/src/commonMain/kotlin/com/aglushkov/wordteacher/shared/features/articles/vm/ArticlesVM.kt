package com.aglushkov.wordteacher.shared.features.articles.vm

import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.exception
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.getErrorString
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.ShortArticle
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Raw
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface ArticlesVM {
    var router: ArticlesRouter?
    val articles: StateFlow<Resource<List<BaseViewItem<*>>>>

    fun onCreateTextArticleClicked()
    fun onArticleClicked(item: ArticleViewItem)
    fun onArticleRemoved(item: ArticleViewItem)

    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?
    fun onTryAgainClicked()

    @Serializable
    class State {
    }
}

open class ArticlesVMImpl(
    val articlesRepository: ArticlesRepository,
    private val idGenerator: IdGenerator,
    private val timeSource: TimeSource,
    private val analytics: Analytics,
): ViewModel(), ArticlesVM {
    override var router: ArticlesRouter? = null

    override val articles = articlesRepository.shortArticles.map {
        //Logger.v("build view items")
        it.copyWith(buildViewItems(it.data() ?: emptyList()))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    override fun onCreateTextArticleClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Articles.createTextArticleClicked"))
        router?.openAddArticle()
    }

    override fun onArticleClicked(item: ArticleViewItem) {
        router?.openArticle(ArticleVM.State(item.articleId))
    }

    override fun onArticleRemoved(item: ArticleViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("Articles.articleRemoved"))
        viewModelScope.launch {
            try {
                articlesRepository.removeArticle(item.articleId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.exception("ArticlesVM.onArticleRemoved", e)
                val errorText = e.message?.let {
                    StringDesc.Raw(it)
                } ?: StringDesc.Resource(MR.strings.error_default)

                // TODO: pass an error message
                //eventChannel.offer(ErrorEvent(errorText))
            }
        }
    }

    private fun buildViewItems(articles: List<ShortArticle>): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()
        articles.forEach {
            items.add(ArticleViewItem(it.id, it.name, timeSource.stringDate(it.date), it.isRead))
        }

        generateIds(items)
        return items
    }

    private fun generateIds(items: MutableList<BaseViewItem<*>>) {
        generateViewItemIds(items, articles.value.data().orEmpty(), idGenerator)
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.articles_error)
    }

    override fun onTryAgainClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Articles.onTryAgainClicked"))
        // TODO: do sth with articlesRepository
    }
}
