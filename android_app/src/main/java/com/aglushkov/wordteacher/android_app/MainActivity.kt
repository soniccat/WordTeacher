package com.aglushkov.wordteacher.android_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.AppBarDefaults.TopAppBarElevation
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.android_app.compose.ComposeAppTheme
import com.aglushkov.wordteacher.android_app.di.AppComponent
import com.aglushkov.wordteacher.android_app.di.AppComponentOwner
import com.aglushkov.wordteacher.android_app.features.learning.views.LearningUI
import com.aglushkov.wordteacher.android_app.features.learning.views.LearningUIDialog
import com.aglushkov.wordteacher.android_app.features.learning_session_result.views.LearningSessionResultUI
import com.aglushkov.wordteacher.android_app.features.learning_session_result.views.LearningSessionResultUIDialog
import com.aglushkov.wordteacher.android_app.features.notes.NotesUI
import com.aglushkov.wordteacher.android_app.helper.EmailOpenerImpl
import com.aglushkov.wordteacher.android_app.helper.FileOpenControllerImpl
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.views.AddArticleUIDialog
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleRouter
import com.aglushkov.wordteacher.shared.features.article.views.ArticleUI
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.articles.views.ArticlesUI
import com.aglushkov.wordteacher.shared.features.cardset.views.CardSetUI
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetRouter
import com.aglushkov.wordteacher.shared.features.cardset_info.views.CardSetInfoUI
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoRouter
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.cardset_json_import.views.CardSetJsonImportUIDialog
import com.aglushkov.wordteacher.shared.features.cardsets.views.CardSetsUI
import com.aglushkov.wordteacher.shared.features.dashboard.DashboardUI
import com.aglushkov.wordteacher.shared.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.shared.features.definitions.views.DefinitionsUIDialog
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsRouter
import com.aglushkov.wordteacher.shared.features.dict_configs.views.DictConfigsUI
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningRouter
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.features.learning.vm.SessionCardResult
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultRouter
import com.aglushkov.wordteacher.shared.features.settings.views.SettingsUI
import com.aglushkov.wordteacher.shared.general.BindSnackbarEventHolder
import com.aglushkov.wordteacher.shared.general.ProvideWindowInsets
import com.aglushkov.wordteacher.shared.general.SimpleRouter
import com.aglushkov.wordteacher.shared.general.SnackbarUI
import com.aglushkov.wordteacher.shared.general.views.slideFromRight
import com.aglushkov.wordteacher.shared.general.withWindowInsetsPadding
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import dev.icerock.moko.resources.desc.StringDesc

class MainActivity : AppCompatActivity(), Router {
    private val bottomBarTabs = listOf(
        ScreenTab.Dashboard,
        ScreenTab.Articles,
        ScreenTab.CardSets,
        ScreenTab.Definitions,
        ScreenTab.Settings
    )

    private lateinit var mainDecomposeComponent: MainDecomposeComponent

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (appComponent().wordFrequencyFileOpenController() as FileOpenControllerImpl).bind(this)
        (appComponent().dslDictOpenController() as FileOpenControllerImpl).bind(this)
        appComponent().googleAuthRepository().bind(this)
        appComponent().vkAuthController().bind(this)
        appComponent().yandexAuthController().bind(this)
        appComponent().webLinkOpenerImpl().bind(this)
        (appComponent().emailOpener() as EmailOpenerImpl).bind(this)
        setupComposeLayout()
        handleIntent()
    }

    private fun handleIntent() {
        intent?.extras?.getLong(EXTRA_ARTICLE_ID, 0L).takeIf { it != 0L }?.let { articleId ->
            mainDecomposeComponent.popToRoot()
            (mainDecomposeComponent.childStack.value.active.instance as? MainDecomposeComponent.Child.Tabs)?.let { tabs ->
                tabs.vm.openArticles()
                mainDecomposeComponent.openArticle(
                    ArticleVM.State(articleId)
                )
            }

        }
        intent = null
    }

    private fun setupComposeLayout() {
        val context = defaultComponentContext()
        val deps = appComponent()
//        deps.routerResolver().setRouter(this)

        mainDecomposeComponent = DaggerMainComposeComponent.builder()
            .setComponentContext(context)
            .setAppComponent(deps)
            .build()
            .mainDecomposeComponent()

        setContent {
            BindSnackbarEventHolder(mainDecomposeComponent) {
                ComposeUI()
            }
        }
    }

    private fun appComponent(): AppComponent =
        (applicationContext as AppComponentOwner).appComponent

    private fun clipboardRepository() = appComponent().clipboardRepository()

    @Composable
    private fun ComposeUI() {
        ComposeAppTheme(isDebug = appComponent().isDebug()) {
            ProvideWindowInsets {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .withWindowInsetsPadding(),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        mainUI()
                        dialogUI()
                    }

                    SnackbarUI()
                }
            }
        }
    }

    @Composable
    private fun mainUI() {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Children(
                stack = mainDecomposeComponent.childStack,
                modifier = Modifier,
                animation = stackAnimation(slideFromRight())
            ) {
                when (val instance = it.instance) {
                    is MainDecomposeComponent.Child.Tabs -> TabsUI(component = instance.vm)
                    is MainDecomposeComponent.Child.Article -> ArticleUI(
                        vm = instance.vm.apply {
                            router = mainDecomposeComponent
                            definitionsVM.router = object : DefinitionsRouter {
                                override fun openCardSets() {
                                    mainDecomposeComponent.openCardSets()
                                }

                                override fun onLocalCardSetUpdated(cardSetId: Long) {
                                    mainDecomposeComponent.onCardSetUpdated(cardSetId)
                                }

                                override fun onDefinitionsClosed() {
                                    // won't happen
                                }
                            }
                        }
                    )
                    is MainDecomposeComponent.Child.CardSet -> CardSetUI(
                        vm = instance.vm.apply {
                            router = object : CardSetRouter {
                                override fun openLearning(state: LearningVM.State) {
                                    mainDecomposeComponent.openLearning(state)
                                }
                                override fun closeCardSet() {
                                    mainDecomposeComponent.back()
                                }
                                override fun openCardSetInfo(state: CardSetInfoVM.State) {
                                    mainDecomposeComponent.openCardSetInfo(state)
                                }
                            }
                        }
                    )
                    is MainDecomposeComponent.Child.CardSetInfo -> CardSetInfoUI(
                        vm = instance.vm.apply {
                            router = object : CardSetInfoRouter {
                                override fun openAddArticle(url: String, showNeedToCreateCardSet: Boolean) {
                                    mainDecomposeComponent.openAddArticle(url, showNeedToCreateCardSet)
                                }

                                override fun onClosed() {
                                    mainDecomposeComponent.back()
                                }
                            }
                        }
                    )
                    is MainDecomposeComponent.Child.CardSets -> CardSetsUI(
                        vm = instance.vm.apply {
                            router = mainDecomposeComponent
                        }, onBackHandler = {
                            mainDecomposeComponent.back()
                        }
                    )
                    is MainDecomposeComponent.Child.Learning -> LearningUI(vm = instance.vm)
                    is MainDecomposeComponent.Child.LearningSessionResult -> LearningSessionResultUI(vm = instance.vm)
                    is MainDecomposeComponent.Child.DictConfigs -> DictConfigsUI(
                        vm = instance.vm,
                        onBackPressed = {
                            mainDecomposeComponent.back()
                        }
                    )
                    else -> throw RuntimeException("mainUI: Not implemented ${instance}")
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun TabsUI(component: TabDecomposeComponent) {
        Scaffold(
            bottomBar = {
                BottomNavigationBarUI(component)
            }
        ) { innerPadding ->
            Children(
                stack = component.childStack,
                animation = stackAnimation(slide())
            ) {
                when (val instance = it.instance) {
                    is TabDecomposeComponent.Child.Dashboard -> DashboardUI(
                        vm = instance.vm.apply {
                            router = mainDecomposeComponent
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                    is TabDecomposeComponent.Child.Definitions -> DefinitionsUI(
                        vm = instance.vm.apply {
                            router = object : DefinitionsRouter {
                                override fun openCardSets() {
                                    mainDecomposeComponent.openCardSets()
                                }

                                override fun onLocalCardSetUpdated(cardSetId: Long) {
                                    mainDecomposeComponent.onCardSetUpdated(cardSetId)
                                }

                                override fun onDefinitionsClosed() {
                                    // won't happen
                                }
                            }
                        },
                        modalModifier = Modifier.padding(innerPadding)
                    )
                    is TabDecomposeComponent.Child.CardSets -> CardSetsUI(
                        vm = instance.vm.apply {
                            router = mainDecomposeComponent
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                    is TabDecomposeComponent.Child.Articles -> ArticlesUI(
                        vm = instance.vm.apply {
                            router = mainDecomposeComponent
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                    is TabDecomposeComponent.Child.Settings -> SettingsUI(
                        vm = instance.vm.apply {
                            router = mainDecomposeComponent
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                    is TabDecomposeComponent.Child.Notes -> NotesUI(
                        vm = instance.vm,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                SnackbarUI()
            }
        }
    }

    @Composable
    private fun dialogUI() {
        val dialogs = mainDecomposeComponent.dialogsStateFlow.subscribeAsState()
        val children = dialogs.value.backStack + dialogs.value.active

        children.onEach { child ->
            when (val instance = child.instance) {
                is MainDecomposeComponent.Child.AddArticle ->
                    AddArticleUIDialog(
                        vm = instance.vm.apply {
                            router = object : AddArticleRouter {
                                override fun onArticleCreated(createdArticleId: Long?) {
                                    mainDecomposeComponent.popDialog(child.configuration) {
                                        if (createdArticleId != null) {
                                            mainDecomposeComponent.onArticleCreated(createdArticleId)
                                        }
                                    }
                                }

                                override fun onError(text: StringDesc) {
                                    mainDecomposeComponent.onError(text)
                                }
                            }
                        },
                    )
                is MainDecomposeComponent.Child.Learning ->
                    LearningUIDialog(
                        vm = instance.vm.apply {
                            router = object : LearningRouter {
                                override fun openLearningSessionResult(results: List<SessionCardResult>) {
                                    mainDecomposeComponent.openLearningSessionResult(results)
                                }

                                override fun openDefinitions(word: String) {
                                    mainDecomposeComponent.openDefinitions(word)
                                }

                                override fun onScreenFinished(
                                    inner: Any,
                                    result: SimpleRouter.Result
                                ) {
                                    mainDecomposeComponent.popDialog(child.configuration)
                                }
                            }
                        }
                    )
                is MainDecomposeComponent.Child.LearningSessionResult -> {
                    LearningSessionResultUIDialog(
                        vm = instance.vm.apply {
                            router = object : LearningSessionResultRouter {
                                override fun onScreenFinished(inner: Any, result: SimpleRouter.Result) {
                                    mainDecomposeComponent.popDialog(child.configuration)
                                }

                                override fun openDefinitions(word: String) {
                                    mainDecomposeComponent.openDefinitions(word)
                                }
                            }
                        }
                    )
                }
                is MainDecomposeComponent.Child.CardSetJsonImport ->
                    CardSetJsonImportUIDialog(
                        vm = instance.vm,
                        onCardSetCreated = {
                            mainDecomposeComponent.popDialog(child.configuration)
                        }
                    )
                is MainDecomposeComponent.Child.Definitions ->
                    DefinitionsUIDialog(
                        vm = instance.vm.apply {
                            router = object : DefinitionsRouter {
                                override fun openCardSets() {
                                    mainDecomposeComponent.openCardSets()
                                }

                                override fun onLocalCardSetUpdated(cardSetId: Long) {
                                    mainDecomposeComponent.onCardSetUpdated(cardSetId)
                                }

                                override fun onDefinitionsClosed() {
                                    mainDecomposeComponent.popDialog(child.configuration)
                                }
                            }
                        },
                    )
                else -> {}
            }
        }
    }

    @Composable
    private fun BottomNavigationBarUI(component: TabDecomposeComponent) {
        val childStack = component.childStack.subscribeAsState()
        val activeChild by remember(childStack) {
            derivedStateOf {
                childStack.value.active
            }
        }
        BottomNavigation(
            modifier = Modifier.requiredHeight(56.dp),
            elevation = TopAppBarElevation
        ) {
            bottomBarTabs.forEachIndexed { index, tab ->
                val isSelected = tab.decomposeChildConfigClass == activeChild.configuration::class.java
                val color = if (isSelected) {
                    if (MaterialTheme.colors.isLight) {
                        LocalContentColor.current
                    } else {
                        MaterialTheme.colors.secondary
                    }
                } else {
                    LocalContentColor.current.copy(alpha = 0.8f)
                }
                BottomNavigationItem(
                    selected = isSelected,
                    onClick = {
                        when (tab) {
                            is ScreenTab.Dashboard -> component.openDashboard()
                            is ScreenTab.Definitions -> component.openDefinitions()
                            is ScreenTab.CardSets -> component.openCardSets()
                            is ScreenTab.Articles -> component.openArticles()
                            is ScreenTab.Settings -> component.openSettings()
                            is ScreenTab.Notes -> component.openNotes()
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = null,
                            tint = color
                        )
                    },
                    label = {
                        Text(
                            stringResource(id = tab.nameRes),
                            color = color
                        )
                    }
                )
            }
        }
    }

    // Router

    override fun openAddArticle() {
        mainDecomposeComponent.openAddArticle()
    }

    override fun openArticle(state: ArticleVM.State) {
        mainDecomposeComponent.openArticle(state)
    }

    override fun closeArticle() {
        mainDecomposeComponent.back()
    }
}

sealed class ScreenTab(@StringRes val nameRes: Int, @DrawableRes val iconRes: Int, val decomposeChildConfigClass: Class<*>) {
    data object Dashboard : ScreenTab(MR.strings.dashboard_title.resourceId, MR.images.tab_dashboard_24.drawableResId, TabDecomposeComponent.ChildConfiguration.DashboardConfiguration::class.java)
    data object Definitions : ScreenTab(MR.strings.tab_definitions.resourceId, R.drawable.ic_field_search_24, TabDecomposeComponent.ChildConfiguration.DefinitionConfiguration::class.java)
    data object CardSets : ScreenTab(MR.strings.tab_learning.resourceId, R.drawable.ic_learning, TabDecomposeComponent.ChildConfiguration.CardSetsConfiguration::class.java)
    data object Articles : ScreenTab(MR.strings.tab_articles.resourceId, R.drawable.ic_tab_article_24, TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration::class.java)
    data object Settings : ScreenTab(MR.strings.tab_settings.resourceId, R.drawable.ic_tab_settings_24, TabDecomposeComponent.ChildConfiguration.SettingsConfiguration::class.java)
    data object Notes : ScreenTab(MR.strings.tab_notes.resourceId, R.drawable.ic_tab_notes, TabDecomposeComponent.ChildConfiguration.NotesConfiguration::class.java)
}

const val EXTRA_ARTICLE_ID = "articleId"