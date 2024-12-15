package com.aglushkov.wordteacher.shared.general

import android.app.Activity
import android.content.res.Resources
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.aglushkov.wordteacher.shared.general.views.pxToDp

@Composable
actual fun CustomDialogUI(
    onDismissRequest: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            decorFitsSystemWindows = false
        )
    ) {
        val window = findDialogWindow() ?: throw Resources.NotFoundException("Window isn't found")
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)

        Surface(
            modifier = Modifier.fillMaxSize().withWindowInsetsPadding(),
            color = MaterialTheme.colors.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                content()
            }
        }
    }
}

@Composable
private fun findDialogWindow(): Window? {
    val view: View = LocalView.current
    var viewParent: ViewParent? = view.parent
    while (viewParent != null && viewParent !is DialogWindowProvider) {
        viewParent = viewParent.parent
    }

    return if (viewParent is DialogWindowProvider) {
        viewParent.window
    } else {
        null
    }
}

fun android.view.WindowInsets.toWindowInsets() = WindowInsets(
    top = systemWindowInsetTop,
    bottom = systemWindowInsetBottom,
    left = systemWindowInsetLeft,
    right = systemWindowInsetRight
)

data class WindowInsets(
    val left: Int = 0,
    val right: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0
) {
    fun log() {
        Logger.v(
            "WindowInsets",
            "left:${left} right:${right} top:${top} bottom:${bottom}"
        )
    }
}

fun Modifier.withWindowInsetsPadding() = composed {
    this.padding(
        start = LocalWindowInsets.current.left.pxToDp(),
        top = LocalWindowInsets.current.top.pxToDp(),
        end = LocalWindowInsets.current.right.pxToDp(),
        bottom = LocalWindowInsets.current.bottom.pxToDp()
    )
}

@Composable
fun Activity.ProvideWindowInsets(
    content: @Composable () -> Unit
) {
    val initialInset = window.decorView.rootWindowInsets.toWindowInsets()
    var windowInsets by remember { mutableStateOf(initialInset) }

    CompositionLocalProvider(
        LocalWindowInsets provides windowInsets,
        content = content
    )

    DisposableEffect("ActivityInsets") {
        window.decorView.setOnApplyWindowInsetsListener { v, insets ->
            windowInsets = insets.toWindowInsets()
            insets.consumeSystemWindowInsets()
        }

        onDispose {
            window.decorView.setOnApplyWindowInsetsListener(null)
        }
    }
}

val LocalWindowInsets = staticCompositionLocalOf { WindowInsets() }
