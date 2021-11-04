package com.aglushkov.wordteacher.androidApp.features.textaction

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.ScreenTab
import com.aglushkov.wordteacher.androidApp.compose.ComposeAppTheme
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.androidApp.features.notes.NotesUI
import com.aglushkov.wordteacher.androidApp.features.textaction.di.DaggerTextActionComponent
import com.aglushkov.wordteacher.androidApp.features.textaction.di.TextActionComponent
import com.aglushkov.wordteacher.androidApp.general.views.compose.slideFromRight
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.textaction.TextActionDecomposeComponent
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.compose.jetpack.Children

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
class TextActionActivity: AppCompatActivity() {
    private lateinit var textActionDecomposeComponent: TextActionDecomposeComponent
    private val bottomBarTabs = listOf(
        ScreenTab.Definitions,
        ScreenTab.Notes
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = defaultComponentContext()
        val deps = (applicationContext as AppComponentOwner).appComponent
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT) // from action.PROCESS_TEXT
            ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT) // from action.ACTION_SEND
        ?: ""

        textActionDecomposeComponent = DaggerTextActionComponent.builder()
            .setAppComponent(deps)
            .setComponentContext(context)
            .setConfig(TextActionComponent.Config(text))
            .build()
            .textActionDecomposeComponent()

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

    @Composable
    private fun MainUI() {
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
                    animation = slideFromRight()
                ) {
                    when (val instance = it.instance) {
                        is TextActionDecomposeComponent.Child.Definitions -> DefinitionsUI(
                            vm = instance.inner,
                            modalModifier = Modifier.padding(innerPadding)
                        )
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
        object Notes : ScreenTab(R.string.tab_notes, R.drawable.ic_tab_notes, TabDecomposeComponent.ChildConfiguration.NotesConfiguration::class.java)
    }
}
