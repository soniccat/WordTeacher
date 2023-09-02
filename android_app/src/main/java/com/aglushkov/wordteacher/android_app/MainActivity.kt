package com.aglushkov.wordteacher.android_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.android_app.compose.ComposeAppTheme
import com.aglushkov.wordteacher.android_app.features.learning.views.LearningUI
import com.aglushkov.wordteacher.android_app.features.learning.views.LearningUIDialog
import com.aglushkov.wordteacher.android_app.features.learning_session_result.views.LearningSessionResultUI
import com.aglushkov.wordteacher.android_app.features.learning_session_result.views.LearningSessionResultUIDialog
import com.aglushkov.wordteacher.android_app.features.notes.NotesUI
import com.aglushkov.wordteacher.shared.general.WindowInsets
import com.aglushkov.wordteacher.android_app.di.AppComponent
import com.aglushkov.wordteacher.android_app.di.AppComponentOwner
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.views.AddArticleUIDialog
import com.aglushkov.wordteacher.shared.features.article.views.ArticleUI
import com.aglushkov.wordteacher.shared.features.articles.views.ArticlesUI
import com.aglushkov.wordteacher.shared.features.cardset.views.CardSetUI
import com.aglushkov.wordteacher.shared.features.cardsets.views.CardSetsUI
import com.aglushkov.wordteacher.shared.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningRouter
import com.aglushkov.wordteacher.shared.features.learning.vm.SessionCardResult
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultRouter
import com.aglushkov.wordteacher.shared.features.settings.views.SettingsUI
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsRouter
import com.aglushkov.wordteacher.shared.general.LocalWindowInset
import com.aglushkov.wordteacher.shared.general.SimpleRouter
import com.aglushkov.wordteacher.shared.general.views.slideFromRight
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.childAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.slide

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalUnitApi
@ExperimentalComposeUiApi
class MainActivity : AppCompatActivity(), Router {
    private val bottomBarTabs = listOf(
        ScreenTab.Definitions,
        ScreenTab.CardSets,
        ScreenTab.Articles,
        ScreenTab.Settings
    )

    private lateinit var mainDecomposeComponent: MainDecomposeComponent

    private var windowInsets by mutableStateOf(WindowInsets())

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.setOnApplyWindowInsetsListener { v, insets ->
            windowInsets = WindowInsets(
                top = insets.systemWindowInsetTop,
                bottom = insets.systemWindowInsetBottom,
                left = insets.systemWindowInsetLeft,
                right = insets.systemWindowInsetRight
            )

            // call default logic
            v.onApplyWindowInsets(insets)
        }

        appComponent().googleAuthRepository().bind(this@MainActivity)
        setupComposeLayout()
        handleIntent()
    }

    private fun handleIntent() {
        intent?.extras?.getLong(EXTRA_ARTICLE_ID, 0L).takeIf { it != 0L }?.let { articleId ->
            mainDecomposeComponent.popToRoot()
            (mainDecomposeComponent.routerState.value.activeChild.instance as? MainDecomposeComponent.Child.Tabs)?.let { tabs ->
                tabs.vm.openArticles()
                mainDecomposeComponent.openArticle(articleId)
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
            ComposeUI()
        }
    }

    private fun appComponent(): AppComponent =
        (applicationContext as AppComponentOwner).appComponent

    @Composable
    private fun ComposeUI() {
        ComposeAppTheme(isDebug = appComponent().isDebug()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                mainUI()

                CompositionLocalProvider(
                    LocalWindowInset provides windowInsets,
                ) {
                    dialogUI()
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
                routerState = mainDecomposeComponent.routerState,
                animation = childAnimation(slideFromRight())
            ) {
                when (val instance = it.instance) {
                    is MainDecomposeComponent.Child.Tabs -> TabsUI(component = instance.vm)
                    is MainDecomposeComponent.Child.Article -> ArticleUI(
                        vm = instance.vm.apply {
                            router = mainDecomposeComponent
                            definitionsVM.router = mainDecomposeComponent
                        }
                    )
                    is MainDecomposeComponent.Child.CardSet -> CardSetUI(vm = instance.vm.apply {
                        router = mainDecomposeComponent
                    })
                    is MainDecomposeComponent.Child.CardSets -> CardSetsUI(vm = instance.vm.apply {
                        router = mainDecomposeComponent
                    }, onBackHandler = {
                        mainDecomposeComponent.back()
                    })
                    is MainDecomposeComponent.Child.Learning -> LearningUI(vm = instance.vm)
                    is MainDecomposeComponent.Child.LearningSessionResult -> LearningSessionResultUI(vm = instance.vm)
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
                routerState = component.routerState,
                animation = childAnimation(slide())
            ) {
                when (val instance = it.instance) {
                    is TabDecomposeComponent.Child.Definitions -> DefinitionsUI(
                        vm = instance.vm.apply {
                            router = mainDecomposeComponent
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
        }
    }

    @Composable
    private fun dialogUI() {
        val dialogs = mainDecomposeComponent.dialogsStateFlow.collectAsState()

        dialogs.value.onEach { instance ->
            when (val instance = instance.instance) {
                is MainDecomposeComponent.Child.AddArticle ->
                    AddArticleUIDialog(
                        vm = instance.vm,
                        onArticleCreated = {
                            mainDecomposeComponent.popDialog(instance)
                        }
                    )
                is MainDecomposeComponent.Child.Learning ->
                    LearningUIDialog(
                        vm = instance.vm.apply {
                            router = object : LearningRouter {
                                override fun openSessionResult(results: List<SessionCardResult>) {
                                    mainDecomposeComponent.openLearningSessionResult(results)
                                }

                                override fun onScreenFinished(
                                    inner: Any,
                                    result: SimpleRouter.Result
                                ) {
                                    mainDecomposeComponent.popDialog(instance)
                                }
                            }
                        }
                    )
                is MainDecomposeComponent.Child.LearningSessionResult -> {
                    LearningSessionResultUIDialog(
                        vm = instance.vm.apply {
                            router = object : LearningSessionResultRouter {
                                override fun onScreenFinished(inner: Any, result: SimpleRouter.Result) {
                                    mainDecomposeComponent.popDialog(instance)
                                }
                            }
                        }
                    )
                }
                else -> {}
            }
        }
    }

    @Composable
    private fun BottomNavigationBarUI(component: TabDecomposeComponent) {
        BottomNavigation(
            modifier = Modifier
                .requiredHeight(56.dp)
        ) {
            bottomBarTabs.forEachIndexed { index, tab ->
                BottomNavigationItem(
                    selected = tab.decomposeChildConfigClass == component.routerState.value.activeChild.configuration::class.java,
                    onClick = {
                        when (tab) {
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
                            tint = LocalContentColor.current
                        )
                    },
                    label = {
                        Text(stringResource(id = tab.nameRes))
                    }
                )
            }
        }
    }

    // Router

    override fun openAddArticle() {
        mainDecomposeComponent.openAddArticle()
    }

    override fun openArticle(id: Long) {
        mainDecomposeComponent.openArticle(id)
    }

    override fun closeArticle() {
        mainDecomposeComponent.back()
    }

    override fun openCardSet(id: Long) {
        mainDecomposeComponent.openCardSet(id)
    }

    override fun openLearning(ids: List<Long>) {
        mainDecomposeComponent.openLearning(ids)
    }

    override fun closeCardSet() {
        mainDecomposeComponent.back()
    }

    override fun openJsonImport() {
        TODO("Not yet implemented")
    }
}

sealed class ScreenTab(@StringRes val nameRes: Int, @DrawableRes val iconRes: Int, val decomposeChildConfigClass: Class<*>) {
    object Definitions : ScreenTab(MR.strings.tab_definitions.resourceId, R.drawable.ic_field_search_24, TabDecomposeComponent.ChildConfiguration.DefinitionConfiguration::class.java)
    object CardSets : ScreenTab(MR.strings.tab_learning.resourceId, R.drawable.ic_learning, TabDecomposeComponent.ChildConfiguration.CardSetsConfiguration::class.java)
    object Articles : ScreenTab(MR.strings.tab_articles.resourceId, R.drawable.ic_tab_article_24, TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration::class.java)
    object Settings : ScreenTab(MR.strings.tab_settings.resourceId, R.drawable.ic_tab_settings_24, TabDecomposeComponent.ChildConfiguration.SettingsConfiguration::class.java)
    object Notes : ScreenTab(MR.strings.tab_notes.resourceId, R.drawable.ic_tab_notes, TabDecomposeComponent.ChildConfiguration.NotesConfiguration::class.java)
}

const val EXTRA_ARTICLE_ID = "articleId"