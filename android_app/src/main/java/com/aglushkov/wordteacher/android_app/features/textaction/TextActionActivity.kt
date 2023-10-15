package com.aglushkov.wordteacher.android_app.features.textaction

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
import androidx.compose.runtime.Composable
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
import com.aglushkov.wordteacher.shared.features.definitions.views.DefinitionsUI
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.childAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.slide
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc

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
        val intentString = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT) // from action.PROCESS_TEXT
            ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT) // from action.ACTION_SEND
        ?: intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.toString() ?: ""

        var text: CharSequence? = null
        var urlString: String? = null

        // TODO: move this login into VM layer
        val matcher = Patterns.WEB_URL.matcher(intentString)
        if (matcher.find()) {
            try {
                urlString = URL(intentString.subSequence(matcher.start(), matcher.end()).toString()).toString()
            } catch (e: Throwable) {
            }
        }

        if (urlString == null) {
            try {
                if (Uri.parse(intentString.toString()).scheme != null) {
                    urlString = intentString.toString()
                }
            } catch (e: Throwable) {
            }
        }

        if (urlString == null) {
            text = intentString
        }

        textActionDecomposeComponent = DaggerTextActionComponent.builder()
            .setAppComponent(deps)
            .setComponentContext(context)
            .setConfig(TextActionComponent.Config(text ?: "", urlString))
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
                    animation = childAnimation(slide())
                ) {
                    when (val instance = it.instance) {
                        is TextActionDecomposeComponent.Child.Definitions -> DefinitionsUI(
                            vm = instance.vm.apply {
                                router = object : DefinitionsRouter {
                                    override fun openCardSets() {
                                        textActionDecomposeComponent.openCardSets()
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
                                                Intent(this@TextActionActivity, MainActivity::class.java).apply {
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
        object Definitions : ScreenTab(MR.strings.tab_definitions.resourceId, MR.images.field_search_24.drawableResId, TextActionDecomposeComponent.ChildConfiguration.DefinitionConfiguration::class.java)
        object AddArticle : ScreenTab(MR.strings.tab_add_article.resourceId, MR.images.create_note.drawableResId, TextActionDecomposeComponent.ChildConfiguration.AddArticleConfiguration::class.java)
        object Notes : ScreenTab(MR.strings.tab_notes.resourceId, R.drawable.ic_tab_notes, TextActionDecomposeComponent.ChildConfiguration.AddNoteConfiguration::class.java)
    }
}
