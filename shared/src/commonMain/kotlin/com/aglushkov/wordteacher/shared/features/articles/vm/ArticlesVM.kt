package com.aglushkov.wordteacher.shared.features.articles.vm

import com.aglushkov.wordteacher.shared.events.CompletionEvent
import com.aglushkov.wordteacher.shared.events.CompletionResult
import com.aglushkov.wordteacher.shared.events.ErrorEvent
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.getErrorString
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.ShortArticle
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import dev.icerock.moko.resources.desc.Raw
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface ArticlesVM {
    var router: ArticlesRouter?
    val articles: StateFlow<Resource<List<BaseViewItem<*>>>>

    fun onCreateTextArticleClicked()
    fun onArticleClicked(item: ArticleViewItem)
    fun onArticleRemoved(item: ArticleViewItem)

    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?
    fun onTryAgainClicked()

    @Parcelize
    class State: Parcelable {
    }
}

open class ArticlesVMImpl(
    val articlesRepository: ArticlesRepository,
    private val timeSource: TimeSource,
): ViewModel(), ArticlesVM {
    override var router: ArticlesRouter? = null

    override val articles = articlesRepository.shortArticles.map {
        //Logger.v("build view items")
        it.copyWith(buildViewItems(it.data() ?: emptyList()))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Uninitialized())

    override fun onCreateTextArticleClicked() {
        router?.openAddArticle()
    }

    override fun onArticleClicked(item: ArticleViewItem) {
        router?.openArticle(item.id)
    }

    override fun onArticleRemoved(item: ArticleViewItem) {
        viewModelScope.launch {
            try {
                articlesRepository.removeArticle(item.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
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
            items.add(ArticleViewItem(it.id, it.name, timeSource.stringDate(it.date)))
        }

        return items
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.articles_error)
    }

    override fun onTryAgainClicked() {
        // TODO: do sth with articlesRepository
    }
}
