package com.aglushkov.wordteacher.shared.features.find_article

import androidx.datastore.preferences.core.Preferences
import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardCategoriesViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardExpandViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardHeadlineViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardVM
import com.aglushkov.wordteacher.shared.features.dashboard.vm.HintViewItem
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsViewTitleItem
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.WebLinkOpener
import com.aglushkov.wordteacher.shared.general.extensions.collectUntilDone
import com.aglushkov.wordteacher.shared.general.extensions.combine7
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateIds
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.settings.HintType
import com.aglushkov.wordteacher.shared.general.settings.isHintClosed
import com.aglushkov.wordteacher.shared.model.ShortArticle
import com.aglushkov.wordteacher.shared.model.ShortCardSet
import com.aglushkov.wordteacher.shared.repository.dashboard.DashboardRepository
import com.aglushkov.wordteacher.shared.repository.dashboard.ReadHeadlineRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.shared.service.SpaceDashboardResponse
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceStringDesc
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.collections.map
import kotlin.collections.orEmpty

interface FindArticleVM: Clearable {
    var router: Router?
    val viewItems: StateFlow<Resource<List<BaseViewItem<*>>>>

    fun onTryAgainClicked()
    fun onHeadlineCategoryChanged(index: Int)
    fun onHeadlineClicked(item: DashboardHeadlineViewItem)
    fun onAddHeadlineClicked(item: DashboardHeadlineViewItem)
    fun onLinkClicked(link: String)
    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?
    fun onClosed()

    interface Router {
        fun openAddArticle(url: String?, showNeedToCreateCardSet: Boolean)
        fun openArticle(state: ArticleVM.State)
        fun onClosed()
    }

    @Serializable
    data class State (
        val selectedCategoryIndex: Int = 0,
    )
}

open class FindArticleVMImpl(
    restoredState: FindArticleVM.State,
    private val dashboardRepository: DashboardRepository,
    private val readHeadlineRepository: ReadHeadlineRepository,
    private val idGenerator: IdGenerator,
    private val analytics: Analytics,
    private val webLinkOpener: WebLinkOpener,
): ViewModel(), FindArticleVM {
    override var router: FindArticleVM.Router? = null
    private val stateFlow = MutableStateFlow(restoredState)
    val state: FindArticleVM.State
        get() = stateFlow.value

    override val viewItems = combine(
        stateFlow,
        dashboardRepository.stateFlow,
        readHeadlineRepository.stateFlow.map { it.data().orEmpty() },
        ::buildViewItems,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Resource.Loading())

    init {
        loadDashboard()
    }

    private fun buildViewItems(
        state: FindArticleVM.State,
        dashboardRes: Resource<SpaceDashboardResponse>,
        readHeadlines: Set<String>,
    ): Resource<List<BaseViewItem<*>>> =
        dashboardRes.mapLoadedData { response ->
            buildList<BaseViewItem<*>> {
                response.headlineBlock.categories.getOrNull(state.selectedCategoryIndex)?.let { selectedCategory ->
                    add(
                        DashboardCategoriesViewItem(
                            categories = response.headlineBlock.categories.map { it.categoryName },
                            selectedIndex = state.selectedCategoryIndex,
                        )
                    )
                    selectedCategory.headlines.onEach { headline ->
                        add(
                            DashboardHeadlineViewItem(
                                id = headline.id,
                                title = headline.title,
                                description = headline.description,
                                sourceName = headline.sourceName,
                                sourceCategory = headline.sourceCategory,
                                date = headline.date,
                                link = headline.link,
                                isRead = readHeadlines.contains(headline.link)
                            )
                        )
                    }
                }
            }.generateIds(viewItems.value.data().orEmpty(), idGenerator)
        }

    override fun onTryAgainClicked() {
        analytics.send(AnalyticEvent.createActionEvent("FindArticle.onTryAgainClicked"))
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            dashboardRepository.reloadIfNeeded()
        }
    }

    override fun onHeadlineCategoryChanged(index: Int) {
        analytics.send(AnalyticEvent.createActionEvent("FindArticle.onHeadlineCategoryChanged"))
        stateFlow.update { it.copy(selectedCategoryIndex = index) }
    }

    override fun onHeadlineClicked(item: DashboardHeadlineViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("FindArticle.onHeadlineClicked"))
        readHeadlineRepository.put(item.link)
        webLinkOpener.open(item.link)
    }

    override fun onAddHeadlineClicked(item: DashboardHeadlineViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("FindArticle.onAddHeadlineClicked"))
        readHeadlineRepository.put(item.link)
        router?.openAddArticle(item.link, true)
    }

    override fun onLinkClicked(link: String) {
        analytics.send(AnalyticEvent.createActionEvent("Dashboard.onLinkClicked"))
        webLinkOpener.open(link)
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.error_default_loading_error)
    }

    override fun onClosed() {
        router?.onClosed()
    }
}
