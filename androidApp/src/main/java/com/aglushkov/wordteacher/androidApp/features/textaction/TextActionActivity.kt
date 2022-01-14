package com.aglushkov.wordteacher.androidApp.features.textaction

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.ComposeAppTheme
import com.aglushkov.wordteacher.androidApp.features.add_article.views.AddArticleUI
import com.aglushkov.wordteacher.androidApp.features.add_article.views.AddArticleUIDialog
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.androidApp.features.notes.NotesUI
import com.aglushkov.wordteacher.androidApp.features.textaction.di.DaggerTextActionComponent
import com.aglushkov.wordteacher.androidApp.features.textaction.di.TextActionComponent
import com.aglushkov.wordteacher.androidApp.general.views.compose.slideFromRight
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsRouter
import com.aglushkov.wordteacher.shared.features.textaction.TextActionDecomposeComponent
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.compose.jetpack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.animation.child.slide
import java.net.URL
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@ExperimentalUnitApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
class TextActionActivity: AppCompatActivity() {
    private lateinit var textActionDecomposeComponent: TextActionDecomposeComponent
    private val bottomBarTabs = listOf(
        ScreenTab.Definitions,
        ScreenTab.AddArticle,
        ScreenTab.Notes
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = defaultComponentContext()
        val deps = (applicationContext as AppComponentOwner).appComponent
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT) // from action.PROCESS_TEXT
            ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT) // from action.ACTION_SEND
        ?: ""

        var urlString: String? = null
        val matcher = Patterns.WEB_URL.matcher(text)
        if (matcher.find()) {
            try {
                urlString = URL(text.subSequence(matcher.start(), matcher.end()).toString()).toString()
            } catch (e: Throwable) {
            }
        }

        textActionDecomposeComponent = DaggerTextActionComponent.builder()
            .setAppComponent(deps)
            .setComponentContext(context)
            .setConfig(TextActionComponent.Config(text, urlString))
            .build()
            .textActionDecomposeComponent()

        if (urlString != null) {
            textActionDecomposeComponent.openAddArticle()
        }

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
                MainUI()
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun MainUI() {
        val coroutineScope = rememberCoroutineScope()

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                bottomBar = {
                    BottomNavigationBarUI(textActionDecomposeComponent)
                }
            ) { innerPadding ->
                Children(
                    routerState = textActionDecomposeComponent.routerState,
                    animation = slide()
                ) {
                    when (val instance = it.instance) {
                        is TextActionDecomposeComponent.Child.Definitions -> DefinitionsUI(
                            vm = instance.inner.apply {
                                router = object : DefinitionsRouter {
                                    override fun openCardSets() {
                                        textActionDecomposeComponent.openCardSets()
                                    }
                                }
                            },
                            modalModifier = Modifier.padding(innerPadding)
                        )
                        is TextActionDecomposeComponent.Child.AddArticle -> {
                            val articleCreatedString = stringResource(id = R.string.articles_action_article_created)
                            AddArticleUI(
                                vm = instance.inner,
                                modifier = Modifier.padding(innerPadding),
                                onArticleCreated = {
                                    coroutineScope.launch {
                                        showSnackbar(articleCreatedString)
                                    }
                                }
                            )
                        }
                        is TextActionDecomposeComponent.Child.AddNote -> NotesUI(
                            vm = instance.inner,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun BottomNavigationBarUI(component: TextActionDecomposeComponent) {
        BottomNavigation(
            modifier = Modifier.requiredHeight(56.dp)
        ) {
            bottomBarTabs.forEachIndexed { index, tab ->
                BottomNavigationItem(
                    selected = tab.decomposeChildConfigClass == component.routerState.value.activeChild.configuration::class.java,
                    onClick = {
                        when (tab) {
                            is ScreenTab.Definitions -> component.openDefinitions()
                            is ScreenTab.AddArticle -> component.openAddArticle()
                            is ScreenTab.Notes -> component.openAddNote()
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

    sealed class ScreenTab(@StringRes val nameRes: Int, @DrawableRes val iconRes: Int, val decomposeChildConfigClass: Class<*>) {
        object Definitions : ScreenTab(R.string.tab_definitions, R.drawable.ic_field_search_24, TextActionDecomposeComponent.ChildConfiguration.DefinitionConfiguration::class.java)
        object AddArticle : ScreenTab(R.string.tab_add_article, R.drawable.ic_create_note, TextActionDecomposeComponent.ChildConfiguration.AddArticleConfiguration::class.java)
        object Notes : ScreenTab(R.string.tab_notes, R.drawable.ic_tab_notes, TextActionDecomposeComponent.ChildConfiguration.AddNoteConfiguration::class.java)
    }
}
