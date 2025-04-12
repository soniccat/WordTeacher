package com.aglushkov.wordteacher.shared.features.dashboard.vm

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.cardsets.vm.RemoteCardSetViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsRouter
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordLoadingViewItem
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsViewTitleItem
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.WebLinkOpener
import com.aglushkov.wordteacher.shared.general.extensions.combine6
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilDone
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.buildSimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resource.on
import com.aglushkov.wordteacher.shared.general.resource.onData
import com.aglushkov.wordteacher.shared.general.resource.onError
import com.aglushkov.wordteacher.shared.general.resource.onLoading
import com.aglushkov.wordteacher.shared.general.toOkResponse
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.ShortArticle
import com.aglushkov.wordteacher.shared.model.ShortCardSet
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.dashboard.ReadCardSetRepository
import com.aglushkov.wordteacher.shared.repository.dashboard.ReadHeadlineRepository
import com.aglushkov.wordteacher.shared.service.SpaceDashboardResponse
import com.aglushkov.wordteacher.shared.service.SpaceDashboardService
import dev.icerock.moko.resources.desc.ResourceStringDesc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface DashboardVM: Clearable {
    var router: Router?
    val viewItems: StateFlow<Resource<List<BaseViewItem<*>>>>
    val state: State

    fun refresh()
    fun onHeadlineCategoryChanged(index: Int)
    fun onHeadlineClicked(item: DashboardHeadlineViewItem)
    fun onAddHeadlineClicked(item: DashboardHeadlineViewItem)
    fun onCardSetClicked(item: RemoteCardSetViewItem)
    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?
    fun onExpandClicked(item: DashboardExpandViewItem)

    interface Router {
        fun openAddArticle(url: String?, showNeedToCreateCardSet: Boolean)
        fun openCardSet(state: CardSetVM.State)
    }

    @Serializable
    data class State (
        val lastLoadDate: Instant? = null,
        val selectedCategoryIndex: Int = 0,
        val isHeadlineBlockExpanded: Boolean = false,
        val isCardSetBlockExpanded: Boolean = false,
    )
}

open class DashboardVMIMpl(
    restoredState: DashboardVM.State,
    spaceDashboardService: SpaceDashboardService,
    private val cardSetsRepository: CardSetsRepository,
    private val articlesRepository: ArticlesRepository,
    private val readHeadlineRepository: ReadHeadlineRepository,
    private val readCardSetRepository: ReadCardSetRepository,
    private val webLinkOpener: WebLinkOpener,
    private val idGenerator: IdGenerator,
    private val timeSource: TimeSource,
    private val analytics: Analytics,
): ViewModel(), DashboardVM {
    override var router: DashboardVM.Router? = null
    private val stateFlow = MutableStateFlow<DashboardVM.State>(restoredState)
    override val state: DashboardVM.State
        get() = stateFlow.value

    private val dashboardRepository: SimpleResourceRepository<SpaceDashboardResponse, Unit> =
        buildSimpleResourceRepository<SpaceDashboardResponse, Unit>(viewModelScope) {
            spaceDashboardService.load().toOkResponse()
        }

    override val viewItems = combine6(
        stateFlow,
        dashboardRepository.stateFlow,
        cardSetsRepository.cardSets,
        articlesRepository.shortArticles,
        readHeadlineRepository.stateFlow.map { it.data().orEmpty() },
        readCardSetRepository.stateFlow.map { it.data().orEmpty() },
        ::buildViewItems,
    ).stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Loading())

    init {
        loadDashboard()
    }

    override fun refresh() {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            dashboardRepository.load(Unit).waitUntilDone().onData {
                stateFlow.update {
                    it.copy(lastLoadDate = timeSource.timeInstant())
                }
            }
        }
    }

    override fun onHeadlineCategoryChanged(index: Int) {
        stateFlow.update { it.copy(selectedCategoryIndex = index) }
    }

    override fun onHeadlineClicked(item: DashboardHeadlineViewItem) {
        readHeadlineRepository.put(item.link)
        webLinkOpener.open(item.link)
    }

    override fun onAddHeadlineClicked(item: DashboardHeadlineViewItem) {
        readHeadlineRepository.put(item.link)
        router?.openAddArticle(item.link, true)
    }

    override fun onCardSetClicked(item: RemoteCardSetViewItem) {
        readCardSetRepository.put(item.remoteCardSetId)
        router?.openCardSet(CardSetVM.State.RemoteCardSet(item.remoteCardSetId))
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.error_default_loading_error)
    }

    override fun onExpandClicked(item: DashboardExpandViewItem) {
        stateFlow.update {
            it.copy(
                isHeadlineBlockExpanded = if (item.firstItem() == DashboardExpandViewItem.ExpandType.Headline) {
                    !it.isHeadlineBlockExpanded
                } else {
                    it.isHeadlineBlockExpanded
                },
                isCardSetBlockExpanded = if (item.firstItem() == DashboardExpandViewItem.ExpandType.CardSets) {
                    !it.isCardSetBlockExpanded
                } else {
                    it.isCardSetBlockExpanded
                },
            )
        }
    }

    private fun buildViewItems(
        state: DashboardVM.State,
        dashboardRes: Resource<SpaceDashboardResponse>,
        cardSetsRes: Resource<List<ShortCardSet>>,
        articlesRes: Resource<List<ShortArticle>>,
        readHeadlineRepository: Set<String>,
        readCardSetRepository: Set<String>,
    ): Resource<List<BaseViewItem<*>>> {
        if (cardSetsRes.isLoading() && cardSetsRes.data() == null) {
            return Resource.Loading()
        }

        if (articlesRes.isLoading() && articlesRes.data() == null) {
            return Resource.Loading()
        }

        // TODO: handle data from cardSetsRes articlesRes

        val resultList = mutableListOf<BaseViewItem<*>>()
        dashboardRes.on(
            data = {
                it.headlineBlock.categories.getOrNull(state.selectedCategoryIndex)?.let { selectedCategory ->
                    resultList.add(
                        SettingsViewTitleItem(
                            ResourceStringDesc(MR.strings.dashboard_headline_title)
                        )
                    )
                    resultList.add(
                        DashboardCategoriesViewItem(
                            categories = it.headlineBlock.categories.map { it.categoryName },
                            selectedIndex = state.selectedCategoryIndex,
                        )
                    )

                    val resultHeadlines = if (state.isHeadlineBlockExpanded) {
                        selectedCategory.headlines
                    } else {
                        selectedCategory.headlines.take(3)
                    }
                    resultHeadlines.onEach { headline ->
                        // TODO: filter by already added articles by link
                        resultList.add(
                            DashboardHeadlineViewItem(
                                id = headline.id,
                                title = headline.title,
                                description = headline.description,
                                sourceName = headline.sourceName,
                                sourceCategory = headline.sourceCategory,
                                date = headline.date,
                                link = headline.link,
                                isRead = readHeadlineRepository.contains(headline.link)
                            )
                        )
                    }
                    resultList.add(
                        DashboardExpandViewItem(
                            expandType = DashboardExpandViewItem.ExpandType.Headline,
                            isExpanded = state.isHeadlineBlockExpanded,
                        )
                    )
                }

                if (it.newCardSetBlock.cardSets.isNotEmpty()) {
                    resultList.add(
                        SettingsViewTitleItem(
                            ResourceStringDesc(MR.strings.dashboard_cardsets_title)
                        )
                    )
                    val resultCardSets = if (state.isCardSetBlockExpanded) {
                        it.newCardSetBlock.cardSets
                    } else {
                        it.newCardSetBlock.cardSets.take(3)
                    }
                    resultCardSets.onEach { cardSet ->
                        // TODO: filter by already added articles by terms
                        resultList.add(
                            RemoteCardSetViewItem(
                                cardSet.remoteId,
                                cardSet.name,
                                cardSet.terms,
                                false,
                                isRead = readCardSetRepository.contains(cardSet.remoteId)
                            )
                        )
                    }
                    resultList.add(
                        DashboardExpandViewItem(
                            expandType = DashboardExpandViewItem.ExpandType.CardSets,
                            isExpanded = state.isCardSetBlockExpanded,
                        )
                    )
                }
            },
            loading = {
                resultList.add(WordLoadingViewItem())
            },
            error = {
                resultList.add(DashboardTryAgainViewItem(
                    errorText = ResourceStringDesc(MR.strings.error_default_loading_error),
                    tryAgainActionText = ResourceStringDesc(MR.strings.error_try_again),
                ))
            }
        )

        generateIds(resultList)
        return Resource.Loaded(resultList)
    }

    private fun generateIds(items: MutableList<BaseViewItem<*>>) {
        generateViewItemIds(items, viewItems.value.data().orEmpty(), idGenerator)
    }
}
