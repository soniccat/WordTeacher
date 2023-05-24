package com.aglushkov.wordteacher.shared.general

import android.content.res.Resources
import android.util.Log
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import com.aglushkov.wordteacher.shared.general.views.pxToDp

@Composable
actual fun CustomDialogUI(
    onDismissRequest: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        val window = findDialogWindow() ?: throw Resources.NotFoundException("Window isn't found")
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)

        val initialInset = LocalWindowInset.current
        var windowInsets by remember { mutableStateOf(initialInset) }

        Surface(
            modifier = Modifier.fillMaxSize().applyWindowInsetsAsPaddings(windowInsets),
            color = MaterialTheme.colors.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                content()
            }
        }

        DisposableEffect("WindowInsets") {
            window.decorView.setOnApplyWindowInsetsListener { v, insets ->
                windowInsets = WindowInsets(
                    top = insets.systemWindowInsetTop,
                    bottom = insets.systemWindowInsetBottom,
                    left = insets.systemWindowInsetLeft,
                    right = insets.systemWindowInsetRight
                )

                insets.consumeSystemWindowInsets()
            }

            onDispose {
                window.decorView.setOnApplyWindowInsetsListener(null)
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

data class WindowInsets(
    val left: Int = 0,
    val right: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0
) {
    fun log() {
        Log.d(
            "WindowInsets",
            "left:${left} right:${right} top:${top} bottom:${bottom}"
        )
    }
}

fun Modifier.applyWindowInsetsAsPaddings(
    windowInsets: WindowInsets
) = composed {
    this.padding(
        start = windowInsets.left.pxToDp(),
        top = windowInsets.top.pxToDp(),
        end = windowInsets.right.pxToDp(),
        bottom = windowInsets.bottom.pxToDp()
    )
}

val LocalWindowInset = staticCompositionLocalOf { WindowInsets() }
