package com.aglushkov.wordteacher.android_app.features.textaction

import android.app.ComponentCaller
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.AppBarDefaults.TopAppBarElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.android_app.EXTRA_ARTICLE_ID
import com.aglushkov.wordteacher.android_app.MainActivity
import com.aglushkov.wordteacher.android_app.compose.ComposeAppTheme
import com.aglushkov.wordteacher.android_app.features.notes.NotesUI
import com.aglushkov.wordteacher.android_app.features.textaction.di.DaggerTextActionComponent
import com.aglushkov.wordteacher.android_app.features.textaction.di.TextActionComponent
import com.aglushkov.wordteacher.android_app.di.AppComponentOwner
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsRouter
import com.aglushkov.wordteacher.shared.features.textaction.TextActionDecomposeComponent
import com.arkivanov.decompose.defaultComponentContext
import java.net.URL
import kotlinx.coroutines.launch
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.shared.features.add_article.views.AddArticleUI
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.shared.general.ProvideWindowInsets
import com.aglushkov.wordteacher.shared.general.withWindowInsetsPadding
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import java.util.regex.Pattern

//@ExperimentalUnitApi
//@ExperimentalMaterialApi
//@ExperimentalAnimationApi
//@ExperimentalComposeUiApi
class TextActionActivity: AppCompatActivity() {
    private lateinit var textActionDecomposeComponent: TextActionDecomposeComponent
    private val bottomBarTabs = listOf(
        ScreenTab.Definitions,
        ScreenTab.AddArticle,
//        ScreenTab.Notes // TODO: remove completely or repair
    )

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        setIntent(intent)
        recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentString = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT) // from action.PROCESS_TEXT
            ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT) // from action.ACTION_SEND
        ?: intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.toString() ?: ""

        var text: CharSequence? = null
        var urlString: String? = null

        // TODO: move this login into VM layer
        // try parse using WEB_URL
        val matcher = Patterns.WEB_URL.matcher(intentString)
        if (matcher.find()) {
            try {
                urlString = URL(
                    intentString.subSequence(matcher.start(), matcher.end()).toString()
                ).toString()
            } catch (e: Throwable) {
            }
        }

        // try parse the whole string
        if (urlString == null) {
            try {
                val scheme = Uri.parse(intentString.toString()).scheme
                if (
                    listOf(
                        ContentResolver.SCHEME_CONTENT,
                        ContentResolver.SCHEME_FILE,
                        ContentResolver.SCHEME_ANDROID_RESOURCE
                    ).contains(scheme)
                ) {
                    urlString = intentString.toString()
                }
            } catch (e: Throwable) {
            }
        }

        if (urlString == null) {
            text = intentString
        }

        // consume state not to restore it, treat TextActionComponent.Config as an actual state
        // do it not to restore state after onNewIntent and just use data from intent
        val storedBundle = Bundle()
        savedStateRegistry.performSave(storedBundle)
        storedBundle.keySet().firstOrNull()?.let {
            storedBundle.getBundle(it)?.keySet()?.onEach { key ->
                savedStateRegistry.consumeRestoredStateForKey(key)
            }
        }

        val context = defaultComponentContext()
        val deps = (applicationContext as AppComponentOwner).appComponent

        textActionDecomposeComponent = DaggerTextActionComponent.builder()
            .setAppComponent(deps)
            .setComponentContext(context)
            .setConfig(TextActionComponent.Config(text?.toString() ?: "", urlString))
            .build()
            .textActionDecomposeComponent()

        if (urlString != null || text?.length?.let { it >= 100 } == true) {
            textActionDecomposeComponent.openAddArticle()
        }

        setContent {
            ComposeUI()
        }
    }

    @Composable
    private fun ComposeUI() {
        ComposeAppTheme {
            ProvideWindowInsets {
                Surface(
                    modifier = Modifier.fillMaxSize().withWindowInsetsPadding(),
                    color = MaterialTheme.colors.background
                ) {
                    MainUI()
                }
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
                    stack = textActionDecomposeComponent.childStack,
                    animation = stackAnimation(slide())
                ) {
                    when (val instance = it.instance) {
                        is TextActionDecomposeComponent.Child.Definitions -> DefinitionsUI(
                            vm = instance.vm.apply {
                                router = object : DefinitionsRouter {
                                    override fun openCardSets() {
                                        textActionDecomposeComponent.openCardSets()
                                    }

                                    override fun openCardSet(state: CardSetVM.State) {
                                        textActionDecomposeComponent.openCardSet(state)
                                    }
                                }
                            },
                            modalModifier = Modifier.padding(innerPadding)
                        )
                        is TextActionDecomposeComponent.Child.AddArticle -> {
                            val articleCreatedString = StringDesc.Resource(MR.strings.articles_action_article_created).toString(LocalContext.current)
                            val openActionText = StringDesc.Resource(MR.strings.articles_action_open).toString(LocalContext.current)
                            AddArticleUI(
                                vm = instance.vm,
                                modifier = Modifier.padding(innerPadding),
                                onArticleCreated = { articleId ->
                                    coroutineScope.launch {
                                        if (showSnackbar(articleCreatedString, actionLabel = openActionText) == SnackbarResult.ActionPerformed) {
                                            this@TextActionActivity.startActivity(
                                                Intent(
                                                    this@TextActionActivity,
                                                    MainActivity::class.java
                                                ).apply {
                                                    articleId?.let {
                                                        putExtra(EXTRA_ARTICLE_ID, it)
                                                    }
                                                }
                                            )
                                            this@TextActionActivity.finish()
                                        }
                                    }
                                }
                            )
                        }
                        is TextActionDecomposeComponent.Child.AddNote -> NotesUI(
                            vm = instance.vm,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun BottomNavigationBarUI(component: TextActionDecomposeComponent) {
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
                            is ScreenTab.Definitions -> component.openDefinitions()
                            is ScreenTab.AddArticle -> component.openAddArticle()
//                            is ScreenTab.Notes -> component.openAddNote()
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

    sealed class ScreenTab(@StringRes val nameRes: Int, @DrawableRes val iconRes: Int, val decomposeChildConfigClass: Class<*>) {
        object Definitions : ScreenTab(MR.strings.tab_definitions.resourceId, MR.images.field_search_24.drawableResId, TextActionDecomposeComponent.ChildConfiguration.DefinitionConfiguration::class.java)
        object AddArticle : ScreenTab(MR.strings.tab_add_article.resourceId, MR.images.create_note.drawableResId, TextActionDecomposeComponent.ChildConfiguration.AddArticleConfiguration::class.java)
//        object Notes : ScreenTab(MR.strings.tab_notes.resourceId, R.drawable.ic_tab_notes, TextActionDecomposeComponent.ChildConfiguration.AddNoteConfiguration::class.java)
    }
}
