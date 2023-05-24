package com.aglushkov.wordteacher.desktopapp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.aglushkov.wordteacher.desktopapp.compose.ComposeAppTheme
import com.aglushkov.wordteacher.desktopapp.di.DaggerAppComponent
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.views.AddArticleUIDialog
import com.aglushkov.wordteacher.shared.features.articles.views.ArticlesUI
import com.aglushkov.wordteacher.shared.features.cardset.views.CardSetUI
import com.aglushkov.wordteacher.shared.features.cardsets.views.CardSetsUI
import com.aglushkov.wordteacher.shared.features.definitions.di.DaggerDefinitionsComposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.di.DefinitionsComposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningRouter
import com.aglushkov.wordteacher.shared.features.learning.vm.SessionCardResult
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultRouter
import com.aglushkov.wordteacher.shared.general.SimpleRouter
import com.aglushkov.wordteacher.shared.general.views.slideFromRight
import com.aglushkov.wordteacher.shared.res.MR
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.childAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.slide
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.parcelable.ParcelableContainer
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.painterResource
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

private val bottomBarTabs = listOf(
    ScreenTab.Definitions,
    ScreenTab.CardSets,
    ScreenTab.Articles,
//    ScreenTab.Settings
)

lateinit var mainDecomposeComponent: MainDecomposeComponent

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    application {
        val lifecycle = LifecycleRegistry()
        val stateKeeper = StateKeeperDispatcher(tryRestoreStateFromFile())
        val decomposeContext = DefaultComponentContext(
            lifecycle = lifecycle,
            stateKeeper = stateKeeper,
        )

//    val root =
//        runOnUiThread {
//            DefaultRootComponent(
//                componentContext = ,
//                featureInstaller = DefaultFeatureInstaller,
//            )
//        }

        val appComponent = DaggerAppComponent.builder()
//        .generalModule(GeneralModule())
            .build()

        mainDecomposeComponent = DaggerMainComposeComponent.builder()
            .setComponentContext(decomposeContext)
            .setAppComponent(appComponent)
            .build()
            .mainDecomposeComponent()

        Window(onCloseRequest = ::exitApplication) {
            ComposeAppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        mainUI()
                        dialogUI()
                    }
                }
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
//                is MainDecomposeComponent.Child.Article -> ArticleUI(
//                    vm = instance.vm.apply {
//                        router = mainDecomposeComponent
//                        definitionsVM.router = mainDecomposeComponent
//                    }
//                )
                is MainDecomposeComponent.Child.CardSet -> CardSetUI(vm = instance.vm.apply {
                    router = mainDecomposeComponent
                })
                is MainDecomposeComponent.Child.CardSets -> CardSetsUI(vm = instance.vm.apply {
                    router = mainDecomposeComponent
                }, onBackHandler = {
                    mainDecomposeComponent.back()
                })
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
//                is TabDecomposeComponent.Child.Settings -> SettingsUI(
//                    vm = instance.vm,
//                    modifier = Modifier.padding(innerPadding)
//                )
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
//                        is ScreenTab.Settings -> component.openSettings()
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
//    object Settings : ScreenTab(R.string.tab_settings, R.drawable.ic_tab_settings_24, TabDecomposeComponent.ChildConfiguration.SettingsConfiguration::class.java)
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
