package com.aglushkov.wordteacher.shared.features.articles.vm

import androidx.datastore.preferences.core.Preferences
import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.dashboard.vm.HintViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import wordteacher.shared.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.exception
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import com.aglushkov.wordteacher.shared.general.settings.HintType
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.general.settings.isHintClosed
import com.aglushkov.wordteacher.shared.general.settings.setHintClosed
import com.aglushkov.wordteacher.shared.model.ShortArticle
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import wordteacher.shared.generated.resources.articles_error
import wordteacher.shared.generated.resources.error_default

interface ArticlesVM {
    var router: ArticlesRouter?
    val articles: StateFlow<Resource<List<BaseViewItem<*>>>>

    fun onCreateTextArticleClicked()
    fun onArticleClicked(item: ArticleViewItem)
    fun onArticleRemoved(item: ArticleViewItem)

    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringResourceWrapper?
    fun onTryAgainClicked()
    fun onHintClicked(hintType: HintType)

    @Serializable
    class State {
    }
}

open class ArticlesVMImpl(
    val articlesRepository: ArticlesRepository,
    private val idGenerator: IdGenerator,
    private val timeSource: TimeSource,
    private val analytics: Analytics,
    private val settingStore: SettingStore,
): ViewModel(), ArticlesVM {
    override var router: ArticlesRouter? = null

    override val articles = combine(articlesRepository.shortArticles, settingStore.prefs) { articles, prefs ->
        //Logger.v("build view items")
        articles.copyWith(buildViewItems(articles.data() ?: emptyList(), prefs))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Resource.Uninitialized())

    override fun onCreateTextArticleClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Articles.createTextArticleClicked"))
        router?.openFindArticle()
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
            } catch (e: Throwable) {
                Logger.exception("ArticlesVM.onArticleRemoved", e)
                val errorText = e.message?.let {
                    StringResourceWrapper.Raw(it)
                } ?: StringResourceWrapper.String(Res.string.error_default)

                // TODO: pass an error message
                //eventChannel.offer(ErrorEvent(errorText))
            }
        }
    }

    private fun buildViewItems(articles: List<ShortArticle>, prefs: Preferences): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()
        if (!prefs.isHintClosed(HintType.Articles)) {
            items.add(HintViewItem(HintType.Articles))
        }

        articles.forEach {
            items.add(ArticleViewItem(it.id, it.name, timeSource.stringDate(it.date), it.isRead))
        }

        generateIds(items)
        return items
    }

    private fun generateIds(items: MutableList<BaseViewItem<*>>) {
        generateViewItemIds(items, articles.value.data().orEmpty(), idGenerator)
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringResourceWrapper? {
        return StringResourceWrapper.String(Res.string.articles_error)
    }

    override fun onTryAgainClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Articles.onTryAgainClicked"))
        // TODO: do sth with articlesRepository
    }

    override fun onHintClicked(hintType: HintType) {
        analytics.send(AnalyticEvent.createActionEvent("Hint_" + hintType.name))
        settingStore.setHintClosed(hintType)
    }
}
