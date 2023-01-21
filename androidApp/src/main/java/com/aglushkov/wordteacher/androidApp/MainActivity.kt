package com.aglushkov.wordteacher.androidApp

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
import androidx.lifecycle.lifecycleScope
import com.aglushkov.wordteacher.androidApp.compose.ComposeAppTheme
import com.aglushkov.wordteacher.androidApp.databinding.ActivityMainBinding
import com.aglushkov.wordteacher.androidApp.features.add_article.views.AddArticleUIDialog
import com.aglushkov.wordteacher.androidApp.features.article.views.ArticleUI
import com.aglushkov.wordteacher.androidApp.features.articles.views.ArticlesUI
import com.aglushkov.wordteacher.androidApp.features.cardset.views.CardSetUI
import com.aglushkov.wordteacher.androidApp.features.cardsets.views.CardSetsUI
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DaggerMainComposeComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.androidApp.features.learning.views.LearningUI
import com.aglushkov.wordteacher.androidApp.features.learning.views.LearningUIDialog
import com.aglushkov.wordteacher.androidApp.features.learning_session_result.views.LearningSessionResultUI
import com.aglushkov.wordteacher.androidApp.features.learning_session_result.views.LearningSessionResultUIDialog
import com.aglushkov.wordteacher.androidApp.features.notes.NotesUI
import com.aglushkov.wordteacher.androidApp.features.settings.views.SettingsUI
import com.aglushkov.wordteacher.androidApp.general.views.compose.WindowInsets
import com.aglushkov.wordteacher.androidApp.general.views.compose.slideFromRight
import com.aglushkov.wordteacher.androidApp.helper.GoogleAuthData
import com.aglushkov.wordteacher.androidApp.helper.GoogleAuthRepository
import com.aglushkov.wordteacher.di.AppComponent
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningRouter
import com.aglushkov.wordteacher.shared.features.learning.vm.SessionCardResult
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultRouter
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsRouter
import com.aglushkov.wordteacher.shared.general.SimpleRouter
import com.aglushkov.wordteacher.shared.general.resource.asLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.compose.jetpack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.animation.child.childAnimation
import com.arkivanov.decompose.extensions.compose.jetpack.animation.child.slide
import com.google.android.gms.auth.api.identity.SignInCredential
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalUnitApi
@ExperimentalComposeUiApi
class MainActivity : AppCompatActivity(), Router {
    lateinit var binding: ActivityMainBinding
    private val bottomBarTabs = listOf(
        ScreenTab.Definitions,
        ScreenTab.CardSets,
        ScreenTab.Articles,
        ScreenTab.Settings
    )

    // TODO: extract logic between googleAuthRepository and spaceAuthRepository into a controller or useCase
    private lateinit var googleAuthRepository: GoogleAuthRepository

    private lateinit var mainDecomposeComponent: MainDecomposeComponent

    private var windowInsets by mutableStateOf(WindowInsets())

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

        googleAuthRepository = GoogleAuthRepository(
            resources.getString(R.string.default_web_client_id)
        ).apply {
            bind(this@MainActivity)
        }.also { repo ->
            lifecycleScope.launch {
                val firstValue = repo.googleSignInCredentialFlow.value
                repo.googleSignInCredentialFlow.collect {
                    val loadedRes = it.asLoaded()
                    if (loadedRes != null && it != firstValue && !loadedRes.data.isSilent) {
                        signInWithGoogleAuthData(loadedRes.data)
                    } else if (it.isUninitialized()) {
                        signOutFromGoogle()
                    }
                }
            }
        }
        setupComposeLayout()
    }

    private fun signInWithGoogleAuthData(authData: GoogleAuthData) {
        val idToken = authData.tokenId ?: return
        appComponent().spaceAuthRepository().auth(SpaceAuthService.NetworkType.Google, idToken)
    }

    private fun signOutFromGoogle() {
        appComponent().spaceAuthRepository().signOut(SpaceAuthService.NetworkType.Google)
    }

    private fun setupComposeLayout() {
        val context = defaultComponentContext()
        val deps = appComponent()
        deps.routerResolver().setRouter(this)

        mainDecomposeComponent = DaggerMainComposeComponent.builder()
            .setComponentContext(context)
            .setAppComponent(deps)
            .build()
            .mainDecomposeComponent()

        setContent {
            ComposeUI()
        }
    }

    private fun appComponent(): AppComponent {
        val deps = (applicationContext as AppComponentOwner).appComponent
        return deps
    }

    @Composable
    private fun ComposeUI() {
        ComposeAppTheme {
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
                            definitionsVM.router = mainDecomposeComponent
                        }
                    )
                    is MainDecomposeComponent.Child.CardSet -> CardSetUI(vm = instance.vm)
                    is MainDecomposeComponent.Child.CardSets -> CardSetsUI(vm = instance.vm)
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
                        vm = instance.vm,
                        modifier = Modifier.padding(innerPadding)
                    )
                    is TabDecomposeComponent.Child.Articles -> ArticlesUI(
                        vm = instance.vm,
                        modifier = Modifier.padding(innerPadding)
                    )
                    is TabDecomposeComponent.Child.Settings -> SettingsUI(
                        vm = instance.vm.apply {
                            router = object : SettingsRouter {
                                override fun openGoogleAuth() {
                                    val googleSignInAccount = googleAuthRepository.googleSignInCredentialFlow.value.asLoaded()
                                    if (googleSignInAccount == null) {
                                        googleAuthRepository.signIn()
                                    } else {
                                        signInWithGoogleAuthData(googleSignInAccount.data)
                                    }
                                }

                                override fun signOutGoogle() {
                                    googleAuthRepository.signOut()
                                }
                            }
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

//    override fun onBackPressed() {
//        if (supportFragmentManager.backStackEntryCount == 1) {
//            finish()
//        } else {
//            super.onBackPressed()
//        }
//    }

    // Router

    override fun openAddArticle() {
        mainDecomposeComponent.openAddArticleDialog()
    }

    override fun openArticle(id: Long) {
        mainDecomposeComponent.openArticle(id)
    }

    override fun closeArticle() {
        mainDecomposeComponent.back()
//        supportFragmentManager.popBackStack()
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
}

sealed class ScreenTab(@StringRes val nameRes: Int, @DrawableRes val iconRes: Int, val decomposeChildConfigClass: Class<*>) {
    object Definitions : ScreenTab(R.string.tab_definitions, R.drawable.ic_field_search_24, TabDecomposeComponent.ChildConfiguration.DefinitionConfiguration::class.java)
    object CardSets : ScreenTab(R.string.tab_learning, R.drawable.ic_learning, TabDecomposeComponent.ChildConfiguration.CardSetsConfiguration::class.java)
    object Articles : ScreenTab(R.string.tab_articles, R.drawable.ic_tab_article_24, TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration::class.java)
    object Settings : ScreenTab(R.string.tab_settings, R.drawable.ic_tab_settings_24, TabDecomposeComponent.ChildConfiguration.SettingsConfiguration::class.java)
    object Notes : ScreenTab(R.string.tab_notes, R.drawable.ic_tab_notes, TabDecomposeComponent.ChildConfiguration.NotesConfiguration::class.java)
}

val LocalWindowInset = staticCompositionLocalOf { WindowInsets() }
