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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.lifecycleScope
import com.aglushkov.wordteacher.androidApp.compose.ComposeAppTheme
import com.aglushkov.wordteacher.androidApp.databinding.ActivityMainBinding
import com.aglushkov.wordteacher.androidApp.features.add_article.views.AddArticleUIDialog
import com.aglushkov.wordteacher.androidApp.features.article.views.ArticleFragment
import com.aglushkov.wordteacher.androidApp.features.article.views.ArticleUI
import com.aglushkov.wordteacher.androidApp.features.articles.views.ArticlesFragment
import com.aglushkov.wordteacher.androidApp.features.articles.views.ArticlesUI
import com.aglushkov.wordteacher.androidApp.features.cardset.views.CardSetUI
import com.aglushkov.wordteacher.androidApp.features.cardsets.views.CardSetsFragment
import com.aglushkov.wordteacher.androidApp.features.cardsets.views.CardSetsUI
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DaggerMainComposeComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsFragment
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.androidApp.features.learning.views.LearningUI
import com.aglushkov.wordteacher.androidApp.features.learning.views.LearningUIDialog
import com.aglushkov.wordteacher.androidApp.features.learning_session_result.views.LearningSessionResultUI
import com.aglushkov.wordteacher.androidApp.features.learning_session_result.views.LearningSessionResultUIDialog
import com.aglushkov.wordteacher.androidApp.features.notes.NotesUI
import com.aglushkov.wordteacher.androidApp.general.views.compose.WindowInsets
import com.aglushkov.wordteacher.androidApp.general.views.compose.slideFromRight
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningRouter
import com.aglushkov.wordteacher.shared.features.learning.vm.SessionCardResult
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultRouter
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.SimpleRouter
import com.aglushkov.wordteacher.shared.general.article_parser.ArticleParser
import com.aglushkov.wordteacher.shared.general.v
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.compose.jetpack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.animation.child.slide
import kotlin.reflect.KClass
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
        ScreenTab.Notes
    )

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

        setupComposeLayout()

        lifecycleScope.launch {
            val buffer = 100 * 1024
            val text = resources.openRawResource(R.raw.sample).buffered(buffer).use { stream ->
                stream.bufferedReader().readText()
            }

            val parser = ArticleParser()
            val article = parser.parse(text)
            Logger.v(article.title.orEmpty())
            Logger.v(article.text.orEmpty())
        }
    }

    private fun setupComposeLayout() {
        val context = defaultComponentContext()
        val deps = (applicationContext as AppComponentOwner).appComponent
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
                animation = slideFromRight()
            ) {
                when (val instance = it.instance) {
                    is MainDecomposeComponent.Child.Tabs -> TabsUI(component = instance.vm)
                    is MainDecomposeComponent.Child.Article -> ArticleUI(vm = instance.vm)
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
                animation = slide()
            ) {
                when (val instance = it.instance) {
                    is TabDecomposeComponent.Child.Definitions -> DefinitionsUI(
                        vm = instance.inner,
                        modalModifier = Modifier.padding(innerPadding)
                    )
                    is TabDecomposeComponent.Child.CardSets -> CardSetsUI(
                        vm = instance.inner,
                        modifier = Modifier.padding(innerPadding)
                    )
                    is TabDecomposeComponent.Child.Articles -> ArticlesUI(
                        vm = instance.inner,
                        modifier = Modifier.padding(innerPadding)
                    )
                    is TabDecomposeComponent.Child.Notes -> NotesUI(
                        vm = instance.inner,
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
                                override val isDialog: Boolean = true

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
                                override val isDialog: Boolean = true

                                override fun onScreenFinished(inner: Any, result: SimpleRouter.Result) {
                                    mainDecomposeComponent.popDialog(instance)
                                }
                            }
                        }
                    )
                }
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

    private fun setupViewLayout() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomBar.setOnNavigationItemSelectedListener {
            val cl = screenClassById(it.itemId)
            openFragment(cl)
            true
        }

        supportFragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                if (DefinitionsFragment::class.java.name == className) {
                    return DefinitionsFragment()
                } else if (ArticlesFragment::class.java.name == className) {
                    return ArticlesFragment()
                } else if (ArticlesFragment::class.java.name == className) {
                    return ArticlesFragment()
                }

                return super.instantiate(classLoader, className)
            }
        }
        supportFragmentManager.addOnBackStackChangedListener {
            supportFragmentManager.fragments.lastOrNull()?.let {
                screenIdByClass(it::class)?.let { itemId ->
                    binding.bottomBar.selectedItemId = itemId
                }
            }
        }

        if (supportFragmentManager.fragments.size == 0) {
            openFragment(DefinitionsFragment::class)
        }
    }

//    override fun onBackPressed() {
//        if (supportFragmentManager.backStackEntryCount == 1) {
//            finish()
//        } else {
//            super.onBackPressed()
//        }
//    }

    private fun openFragment(cl: KClass<*>, arguments: Bundle? = null, isFullscreen: Boolean = false) {
        val tag = screenNameByClass(cl)
        val fragment = supportFragmentManager.findFragmentByTag(tag)
        val topFragment = supportFragmentManager.fragments.lastOrNull()
        if (fragment == null) {
            val newFragment = supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                cl.java.name
            )
            newFragment.arguments = arguments

            val container = if (isFullscreen) binding.fragmentContainerFullscreen else binding.fragmentContainer
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .addToBackStack(tag)
                .replace(container.id, newFragment, tag)
                .commitAllowingStateLoss()

        } else if (topFragment == null || topFragment::class != cl) {
            supportFragmentManager.popBackStack(tag, 0)
        }
    }

    private fun screenClassById(id: Int): KClass<*> = when(id) {
        R.id.tab_definitions -> DefinitionsFragment::class
        R.id.tab_articles -> ArticlesFragment::class
        R.id.tab_cardsets -> CardSetsFragment::class
        else -> throw IllegalArgumentException("Wrong screen id $id")
    }

    private fun screenIdByClass(cl: KClass<*>): Int? = when(cl) {
        DefinitionsFragment::class -> R.id.tab_definitions
        ArticlesFragment::class -> R.id.tab_articles
        CardSetsFragment::class -> R.id.tab_cardsets
        else -> null
    }

    private fun screenNameByClass(cl: KClass<*>): String = when(cl) {
        DefinitionsFragment::class -> "definitions"
        ArticlesFragment::class -> "articles"
        ArticleFragment::class -> "article"
        CardSetsFragment::class -> "cardsets"
        else -> throw IllegalArgumentException("Wrong screen class $cl")
    }

    private fun openDialogFragment(cl: KClass<*>) {
        val tag = screenNameByClass(cl)
        val newFragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            cl.java.name
        ) as DialogFragment

        newFragment.show(supportFragmentManager, tag)
    }

    // Router

    override fun openAddArticle() {
        mainDecomposeComponent.openAddArticleDialog()
//        openDialogFragment(AddArticleFragment::class)
    }

    override fun openArticle(id: Long) {
        mainDecomposeComponent.openArticle(id)
//        openFragment(ArticleFragment::class, ArticleFragment.createArguments(id), true)
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

    override fun openCardSets() {
        mainDecomposeComponent.openCardSets()
    }
}

sealed class ScreenTab(@StringRes val nameRes: Int, @DrawableRes val iconRes: Int, val decomposeChildConfigClass: Class<*>) {
    object Definitions : ScreenTab(R.string.tab_definitions, R.drawable.ic_field_search_24, TabDecomposeComponent.ChildConfiguration.DefinitionConfiguration::class.java)
    object CardSets : ScreenTab(R.string.tab_learning, R.drawable.ic_learning, TabDecomposeComponent.ChildConfiguration.CardSetsConfiguration::class.java)
    object Articles : ScreenTab(R.string.tab_articles, R.drawable.ic_tab_article_24, TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration::class.java)
    object Notes : ScreenTab(R.string.tab_notes, R.drawable.ic_tab_notes, TabDecomposeComponent.ChildConfiguration.NotesConfiguration::class.java)
}

val LocalWindowInset = staticCompositionLocalOf { WindowInsets() }