package com.aglushkov.wordteacher.shared.features.dashboard.vm

import androidx.datastore.preferences.core.Preferences
import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.RemoteCardSetViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordLoadingViewItem
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsViewTitleItem
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.WebLinkOpener
import com.aglushkov.wordteacher.shared.general.extensions.combine7
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilDone
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.buildSimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resource.on
import com.aglushkov.wordteacher.shared.general.resource.onData
import com.aglushkov.wordteacher.shared.general.serialization.InstantIso8601Serializer
import com.aglushkov.wordteacher.shared.general.settings.HintType
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.general.settings.isHintClosed
import com.aglushkov.wordteacher.shared.general.settings.setHintClosed
import com.aglushkov.wordteacher.shared.general.toOkResponse
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
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Contextual
import kotlin.time.ExperimentalTime

interface DashboardVM: Clearable {
    var router: Router?
    val viewItems: StateFlow<Resource<List<BaseViewItem<*>>>>
    val state: State

    fun onHeadlineCategoryChanged(index: Int)
    fun onHeadlineClicked(item: DashboardHeadlineViewItem)
    fun onAddHeadlineClicked(item: DashboardHeadlineViewItem)
    fun onCardSetClicked(item: CardSetViewItem)
    fun onCardSetStartLearningClicked(item: CardSetViewItem)
    fun onArticleClicked(item: ArticleViewItem)
    fun onRemoteCardSetClicked(item: RemoteCardSetViewItem)
    fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc?
    fun onExpandClicked(item: DashboardExpandViewItem)
    fun onLinkClicked(link: String)
    fun onDashboardTryAgainClicked()
    fun onHintClicked(hintType: HintType)

    interface Router {
        fun openAddArticle(url: String?, showNeedToCreateCardSet: Boolean)
        fun openCardSet(state: CardSetVM.State)
        fun openArticle(state: ArticleVM.State)
        fun openLearning(state: LearningVM.State)
    }

    @Serializable
    data class State (
        @Serializable(with = InstantIso8601Serializer::class)
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
    private val settingStore: SettingStore,
): ViewModel(), DashboardVM {
    override var router: DashboardVM.Router? = null
    private val stateFlow = MutableStateFlow<DashboardVM.State>(restoredState)
    override val state: DashboardVM.State
        get() = stateFlow.value

    private val dashboardRepository: SimpleResourceRepository<SpaceDashboardResponse, Unit> =
        buildSimpleResourceRepository<SpaceDashboardResponse, Unit>(viewModelScope) {
            spaceDashboardService.load().toOkResponse()
        }

    override val viewItems = combine7(
        stateFlow,
        dashboardRepository.stateFlow,
        cardSetsRepository.cardSets,
        articlesRepository.shortArticles,
        readHeadlineRepository.stateFlow.map { it.data().orEmpty() },
        readCardSetRepository.stateFlow.map { it.data().orEmpty() },
        settingStore.prefs,
        ::buildViewItems,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Resource.Loading())

    init {
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
        analytics.send(AnalyticEvent.createActionEvent("Dashboard.onHeadlineCategoryChanged"))
        stateFlow.update { it.copy(selectedCategoryIndex = index) }
    }

    override fun onHeadlineClicked(item: DashboardHeadlineViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("Dashboard.onHeadlineClicked"))
        readHeadlineRepository.put(item.link)
        webLinkOpener.open(item.link)
    }

    override fun onAddHeadlineClicked(item: DashboardHeadlineViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("Dashboard.onAddHeadlineClicked"))
        readHeadlineRepository.put(item.link)
        router?.openAddArticle(item.link, true)
    }

    override fun onCardSetClicked(item: CardSetViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("Dashboard.onCardSetClicked"))
        router?.openCardSet(CardSetVM.State.LocalCardSet(item.cardSetId))
    }

    override fun onCardSetStartLearningClicked(item: CardSetViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("Dashboard.onCardSetStartLearningClicked"))
        router?.openLearning(LearningVM.State(cardSetId = item.cardSetId))
    }

    override fun onArticleClicked(item: ArticleViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("Dashboard.onArticleClicked"))
        router?.openArticle(ArticleVM.State(item.articleId))
    }

    override fun onRemoteCardSetClicked(item: RemoteCardSetViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("Dashboard.onRemoteCardSetClicked"))
        readCardSetRepository.put(item.remoteCardSetId)
        router?.openCardSet(CardSetVM.State.RemoteCardSet(item.remoteCardSetId))
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return StringDesc.Resource(MR.strings.error_default_loading_error)
    }

    override fun onExpandClicked(item: DashboardExpandViewItem) {
        when (item.firstItem()) {
            DashboardExpandViewItem.ExpandType.CardSets ->
                analytics.send(AnalyticEvent.createActionEvent("Dashboard.onCardSetExpandClicked"))
            DashboardExpandViewItem.ExpandType.Headline ->
                analytics.send(AnalyticEvent.createActionEvent("Dashboard.onHeadlineExpandClicked"))
        }

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

    override fun onLinkClicked(link: String) {
        analytics.send(AnalyticEvent.createActionEvent("Dashboard.onLinkClicked"))
        webLinkOpener.open(link)
    }

    override fun onDashboardTryAgainClicked() {
        analytics.send(AnalyticEvent.createActionEvent("Dashboard.onDashboardTryAgainClicked"))
        loadDashboard()
    }

    override fun onHintClicked(hintType: HintType) {
        analytics.send(AnalyticEvent.createActionEvent("Hint_" + hintType.name))
        settingStore.setHintClosed(hintType)
    }

    private fun buildViewItems(
        state: DashboardVM.State,
        dashboardRes: Resource<SpaceDashboardResponse>,
        cardSetsRes: Resource<List<ShortCardSet>>,
        articlesRes: Resource<List<ShortArticle>>,
        readHeadlineRepository: Set<String>,
        readCardSetRepository: Set<String>,
        prefs: Preferences,
    ): Resource<List<BaseViewItem<*>>> {
        if (cardSetsRes.isLoading() && cardSetsRes.data() == null) {
            return Resource.Loading()
        }

        if (articlesRes.isLoading() && articlesRes.data() == null) {
            return Resource.Loading()
        }

        val resultList = mutableListOf<BaseViewItem<*>>()

        if (!prefs.isHintClosed(HintType.Introduction)) {
            resultList.add(HintViewItem(HintType.Introduction))
        }

        val wereInitialHintsClosed = prefs.isHintClosed(HintType.DashboardArticles) &&
                prefs.isHintClosed(HintType.DashboardCardSets)

        // ready to learn card sets
        cardSetsRes.data()?.filter {
                it.terms.isNotEmpty() && it.readyToLearnProgress < 1.0
            }?.sortedByDescending { it.modificationDate }
            ?.take(3)
            ?.takeIf { it.isNotEmpty() }
            ?.let { cardSets ->
                resultList.add(
                    SettingsViewTitleItem(
                        ResourceStringDesc(MR.strings.dashboard_ready_to_learn_cardsets_title)
                    )
                )
                if (wereInitialHintsClosed && !prefs.isHintClosed(HintType.DashboardUsersCardSets)) {
                    resultList.add(HintViewItem(HintType.DashboardUsersCardSets))
                }
                cardSets.onEach { cardSet ->
                    resultList.add(
                        cardSetViewItem(cardSet)
                    )
                }
            }

        // recently started articles
        if (cardSetsRes.isLoadedOrError()) {
            articlesRes.data()?.filter {
                !it.isRead
            }?.sortedByDescending { it.date }
                ?.take(3)
                ?.takeIf { it.isNotEmpty() }
                ?.let { articles ->
                    resultList.add(
                        SettingsViewTitleItem(
                            ResourceStringDesc(MR.strings.dashboard_reading_articles)
                        )
                    )
                    if (wereInitialHintsClosed && !prefs.isHintClosed(HintType.DashboardUsersArticles)) {
                        resultList.add(HintViewItem(HintType.DashboardUsersArticles))
                    }
                    articles.onEach { article ->
                        resultList.add(
                            ArticleViewItem(
                                article.id,
                                article.name,
                                timeSource.stringDate(article.date),
                                article.isRead
                            )
                        )
                    }
                }
        }

        // dashboard content
        dashboardRes.on(
            data = {
                if (cardSetsRes.isLoading() || articlesRes.isLoading()) {
                    return@on
                }

                it.headlineBlock.categories.getOrNull(state.selectedCategoryIndex)?.let { selectedCategory ->
                    resultList.add(
                        SettingsViewTitleItem(
                            ResourceStringDesc(MR.strings.dashboard_headline_title)
                        )
                    )
                    if (!prefs.isHintClosed(HintType.DashboardArticles)) {
                        resultList.add(HintViewItem(HintType.DashboardArticles))
                    }
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
                    if (!prefs.isHintClosed(HintType.DashboardCardSets)) {
                        resultList.add(HintViewItem(HintType.DashboardCardSets))
                    }
                    val resultCardSets = if (state.isCardSetBlockExpanded) {
                        it.newCardSetBlock.cardSets
                    } else {
                        it.newCardSetBlock.cardSets.take(3)
                    }
                    resultCardSets.onEach { cardSet ->
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
                if (resultList.isNotEmpty()) {
                    resultList.add(WordLoadingViewItem())
                }
            },
            error = {
                resultList.add(DashboardTryAgainViewItem(
                    errorText = ResourceStringDesc(MR.strings.error_default_loading_error),
                    tryAgainActionText = ResourceStringDesc(MR.strings.error_try_again),
                ))
            }
        )

        if (resultList.isEmpty()) {
            return Resource.Loading()
        }

        generateIds(resultList)
        return Resource.Loaded(resultList)
    }

    private fun cardSetViewItem(shortCardSet: ShortCardSet): CardSetViewItem {
        return CardSetViewItem(
            shortCardSet.id,
            shortCardSet.name,
            timeSource.stringDate(shortCardSet.creationDate),
            shortCardSet.readyToLearnProgress,
            shortCardSet.totalProgress,
            shortCardSet.terms
        )
    }

    private fun generateIds(items: MutableList<BaseViewItem<*>>) {
        generateViewItemIds(items, viewItems.value.data().orEmpty(), idGenerator)
    }
}
