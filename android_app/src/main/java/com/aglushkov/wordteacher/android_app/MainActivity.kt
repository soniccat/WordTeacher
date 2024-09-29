package com.aglushkov.wordteacher.android_app

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
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
import com.aglushkov.wordteacher.android_app.helper.EmailOpenerImpl
import com.aglushkov.wordteacher.android_app.helper.FileOpenControllerImpl
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.views.AddArticleUIDialog
import com.aglushkov.wordteacher.shared.features.article.views.ArticleUI
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.articles.views.ArticlesUI
import com.aglushkov.wordteacher.shared.features.cardset.views.CardSetUI
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetRouter
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardset_info.views.CardSetInfoUI
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoRouter
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.cardsets.views.CardSetsUI
import com.aglushkov.wordteacher.shared.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.shared.features.dict_configs.views.DictConfigsUI
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
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    override fun onNewIntent(intent: Intent) {
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

        (appComponent().wordFrequencyFileOpenController() as FileOpenControllerImpl).bind(this)
        (appComponent().dslDictOpenController() as FileOpenControllerImpl).bind(this)
        appComponent().googleAuthRepository().bind(this)
        appComponent().vkAuthController().bind(this)
        (appComponent().emailOpener() as EmailOpenerImpl).bind(this)
        setupComposeLayout()
        handleIntent()
    }

    private var lastPrimaryClipDescription: ClipDescription? = null

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(200)
            handleClipboard()
        }
    }

    private fun handleClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!clipboard.hasPrimaryClip()) return

        val primaryDescription = clipboard.primaryClipDescription ?: return
        if (!primaryDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
            !primaryDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
            return
        }
        if (lastPrimaryClipDescription != primaryDescription) {
            lastPrimaryClipDescription = primaryDescription

            val primaryClip = clipboard.primaryClip ?: return
            if (primaryClip.itemCount == 0) {
                return
            }
            val firstItem = primaryClip.getItemAt(0)

            lifecycleScope.launch(Dispatchers.Default) {
                val itemText = firstItem.coerceToText(this@MainActivity)
                if (itemText.isNotEmpty()) {
                    appComponent().clipboardRepository().setText(itemText.toString())
                }
            }
        }
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
                stack = mainDecomposeComponent.childStack,
                modifier = Modifier,
                animation = stackAnimation(slideFromRight())
            ) {
                when (val instance = it.instance) {
                    is MainDecomposeComponent.Child.Tabs -> TabsUI(component = instance.vm)
                    is MainDecomposeComponent.Child.Article -> ArticleUI(
                        vm = instance.vm.apply {
                            router = mainDecomposeComponent
                            definitionsVM.router = mainDecomposeComponent
                        }
                    )
                    is MainDecomposeComponent.Child.CardSet -> CardSetUI(
                        vm = instance.vm.apply {
                            router = object : CardSetRouter {
                                override fun openLearning(ids: List<Long>) {
                                    mainDecomposeComponent.openLearning(ids)
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
                                override fun onClosed() {
                                    mainDecomposeComponent.back()
                                }
                            }
                        }
                    )
                    is MainDecomposeComponent.Child.CardSets -> CardSetsUI(vm = instance.vm.apply {
                        router = mainDecomposeComponent
                    }, onBackHandler = {
                        mainDecomposeComponent.back()
                    })
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
        val dialogs = mainDecomposeComponent.dialogsStateFlow.subscribeAsState()
        val children = listOf(dialogs.value.active) + dialogs.value.backStack

        children.onEach { child ->
            when (val instance = child.instance) {
                is MainDecomposeComponent.Child.AddArticle ->
                    AddArticleUIDialog(
                        vm = instance.vm,
                        onArticleCreated = {
                            mainDecomposeComponent.popDialog(child.configuration)
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
        val childStack = component.childStack.subscribeAsState()
        val activeChild by remember(childStack) {
            derivedStateOf {
                childStack.value.active
            }
        }
        BottomNavigation(
            modifier = Modifier
                .requiredHeight(56.dp)
        ) {
            bottomBarTabs.forEachIndexed { index, tab ->
                BottomNavigationItem(
                    selected = tab.decomposeChildConfigClass == activeChild.configuration::class.java,
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

    override fun openArticle(state: ArticleVM.State) {
        mainDecomposeComponent.openArticle(state)
    }

    override fun closeArticle() {
        mainDecomposeComponent.back()
    }

    override fun openCardSet(state: CardSetVM.State) {
        mainDecomposeComponent.openCardSet(state)
    }

    override fun openLearning(ids: List<Long>) {
        mainDecomposeComponent.openLearning(ids)
    }

    override fun openJsonImport() {
        TODO("That's only desktop feature")
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