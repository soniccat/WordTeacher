package com.aglushkov.wordteacher.desktopapp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.AppBarDefaults.TopAppBarElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import com.aglushkov.wordteacher.desktopapp.compose.ComposeAppTheme
import com.aglushkov.wordteacher.desktopapp.di.DaggerAppComponent
import com.aglushkov.wordteacher.desktopapp.features.webauth.WebAuthUI
import com.aglushkov.wordteacher.desktopapp.helper.FileOpenControllerImpl
import com.aglushkov.wordteacher.desktopapp.helper.GoogleAuthControllerImpl
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.views.AddArticleUIDialog
import com.aglushkov.wordteacher.shared.features.article.views.ArticleUI
import com.aglushkov.wordteacher.shared.features.articles.views.ArticlesUI
import com.aglushkov.wordteacher.shared.features.cardset.views.CardSetUI
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetRouter
import com.aglushkov.wordteacher.shared.features.cardset_info.views.CardSetInfoUI
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoRouter
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.cardset_json_import.views.CardSetJsonImportUIDialog
import com.aglushkov.wordteacher.shared.features.cardsets.views.CardSetsUI
import com.aglushkov.wordteacher.shared.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.shared.features.dict_configs.views.DictConfigsUI
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningRouter
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.features.learning.vm.SessionCardResult
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultRouter
import com.aglushkov.wordteacher.shared.features.settings.views.SettingsUI
import com.aglushkov.wordteacher.shared.features.webauth.vm.WebAuthVM
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.SimpleRouter
import com.aglushkov.wordteacher.shared.general.views.slideFromRight
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.parcelable.ParcelableContainer
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.painterResource
import javafx.application.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.slide

lateinit var mainDecomposeComponent: MainDecomposeComponent
val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

private val bottomBarTabs = listOf(
    ScreenTab.Definitions,
    ScreenTab.CardSets,
    ScreenTab.Articles,
    ScreenTab.Settings
)

fun main() {
    System.setProperty("apple.awt.application.appearance", "system") // for system light/dark mode

    application {
        val lifecycle = LifecycleRegistry()
        val stateKeeper = StateKeeperDispatcher(tryRestoreStateFromFile())
        val decomposeContext = DefaultComponentContext(
            lifecycle = lifecycle,
            stateKeeper = stateKeeper,
        )
        val appComponent = DaggerAppComponent.builder()
            .build()

        Logger().setupDebug(
            StaticConfig(
                Severity.Verbose,
                buildList {
                    if (appComponent.isDebug()) {
                        add(CommonWriter())
                    }
                    add(appComponent.fileLogger())
                }
            )
        )

        // declared here to force initialization on startup
        var databaseCardWorker = appComponent.databaseCardSetWorker()
        var cookieStorage = appComponent.cookieStorage()

        mainScope.launch(Dispatchers.Default) {
            appComponent.nlpCore().load()
        }

        mainDecomposeComponent = DaggerMainComposeComponent.builder()
            .setComponentContext(decomposeContext)
            .setAppComponent(appComponent)
            .build()
            .mainDecomposeComponent()

        (appComponent.googleAuthController() as GoogleAuthControllerImpl).apply {
            authOpener = mainDecomposeComponent
        }

        Window(onCloseRequest = {
            Platform.exit() // JavaFX exit
            exitApplication()
        }) {
            ComposeAppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        mainUI()
                        dialogUI()
                    }
                }
            }
            LaunchedEffect("wordFrequencyFileOpenController init") {
                (appComponent.wordFrequencyFileOpenController() as FileOpenControllerImpl).parent = window
                (appComponent.dslDictOpenController() as FileOpenControllerImpl).parent = window
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
                is MainDecomposeComponent.Child.CardSet -> CardSetUI(vm = instance.vm.apply {
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
                })
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
                is MainDecomposeComponent.Child.DictConfigs -> DictConfigsUI(
                    vm = instance.vm,
                    onBackPressed = {
                        mainDecomposeComponent.back()
                    }
                )
//                is MainDecomposeComponent.Child.Learning -> LearningUI(vm = instance.vm)
//                is MainDecomposeComponent.Child.LearningSessionResult -> LearningSessionResultUI(vm = instance.vm)
                else -> throw RuntimeException("mainUI: Not implemented ${instance}")
            }
        }
    }
}

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
//                is TabDecomposeComponent.Child.Notes -> NotesUI(
//                    vm = instance.vm,
//                    modifier = Modifier.padding(innerPadding)
//                )
                else -> {
                    Text("Unknown screen: $instance")
                }
            }
        }
    }
}


@Composable
private fun dialogUI() {
    val dialogs = mainDecomposeComponent.dialogsStateFlow.subscribeAsState()
    val children = listOf(dialogs.value.active) + dialogs.value.backStack

    children.onEach { ins ->
        when (val instance = ins.instance) {
            is MainDecomposeComponent.Child.AddArticle ->
                AddArticleUIDialog(
                    vm = instance.vm,
                    onArticleCreated = {
                        mainDecomposeComponent.popDialog(ins.configuration)
                    }
                )
            is MainDecomposeComponent.Child.WebAuth ->
                WebAuthUI(
                    vm = instance.vm,
                    onCompleted = {
                        mainDecomposeComponent.popDialog(ins.configuration)
                    }
                )
            is MainDecomposeComponent.Child.CardSetJsonImport ->
                CardSetJsonImportUIDialog(
                    vm = instance.vm,
                    onCardSetCreated = {
                        mainDecomposeComponent.popDialog(ins.configuration)
                    }
                )
//            is MainDecomposeComponent.Child.Learning ->
//                LearningUIDialog(
//                    vm = instance.vm.apply {
//                        router = object : LearningRouter {
//                            override fun openSessionResult(results: List<SessionCardResult>) {
//                                mainDecomposeComponent.openLearningSessionResult(results)
//                            }
//
//                            override fun onScreenFinished(
//                                inner: Any,
//                                result: SimpleRouter.Result
//                            ) {
//                                mainDecomposeComponent.popDialog(instance)
//                            }
//                        }
//                    }
//                )
//            is MainDecomposeComponent.Child.LearningSessionResult -> {
//                LearningSessionResultUIDialog(
//                    vm = instance.vm.apply {
//                        router = object : LearningSessionResultRouter {
//                            override fun onScreenFinished(inner: Any, result: SimpleRouter.Result) {
//                                mainDecomposeComponent.popDialog(instance)
//                            }
//                        }
//                    }
//                )
//            }
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
            .requiredHeight(56.dp),
        elevation = TopAppBarElevation
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
//                        is ScreenTab.Notes -> component.openNotes()
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
                    Text(tab.nameRes.localized())
                }
            )
        }
    }
}

sealed class ScreenTab(val nameRes: StringResource, val iconRes: ImageResource, val decomposeChildConfigClass: Class<*>) {
    object Definitions : ScreenTab(MR.strings.tab_definitions, MR.images.field_search_24, TabDecomposeComponent.ChildConfiguration.DefinitionConfiguration::class.java)
    object CardSets : ScreenTab(MR.strings.tab_learning, MR.images.learning, TabDecomposeComponent.ChildConfiguration.CardSetsConfiguration::class.java)
    object Articles : ScreenTab(MR.strings.tab_articles, MR.images.tab_article_24, TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration::class.java)
    object Settings : ScreenTab(MR.strings.tab_settings, MR.images.tab_settings_24, TabDecomposeComponent.ChildConfiguration.SettingsConfiguration::class.java)
//    object Notes : ScreenTab(R.string.tab_notes, R.drawable.ic_tab_notes, TabDecomposeComponent.ChildConfiguration.NotesConfiguration::class.java)
}

private const val SAVED_STATE_FILE_NAME = "saved_state.dat"

private fun saveStateToFile(state: ParcelableContainer) {
    ObjectOutputStream(File(SAVED_STATE_FILE_NAME).outputStream()).use { output ->
        output.writeObject(state)
    }
}

private fun tryRestoreStateFromFile(): ParcelableContainer? =
    File(SAVED_STATE_FILE_NAME).takeIf(File::exists)?.let { file ->
        try {
            ObjectInputStream(file.inputStream()).use(ObjectInputStream::readObject) as ParcelableContainer
        } catch (e: Exception) {
            null
        } finally {
            file.delete()
        }
    }
