package com.aglushkov.wordteacher.shared.general

import android.app.Activity
import android.content.res.Resources
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.aglushkov.wordteacher.shared.general.views.pxToDp
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

@Composable
actual fun CustomDialogUI(
    onDismissRequest: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        val window = findDialogWindow() ?: throw Resources.NotFoundException("Window isn't found")
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        window.setDimAmount(0.0f)

        var animationProgress: Float by remember { mutableFloatStateOf( 0.0f) }
        PredictiveBackHandler { progress: Flow<BackEventCompat> ->
            try {
                progress.collect { backEvent ->
                    animationProgress = backEvent.progress
                }
                animationProgress = 1F
                onDismissRequest()
            } catch (e: CancellationException) {
                animationProgress = 0F
                throw e
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize()
                .graphicsLayer {
                    translationY = - animationProgress * 5.dp.toPx()
                    translationX = animationProgress * 25.dp.toPx()
                    alpha = 1 - animationProgress
                    scaleX = 1 - (animationProgress / 10F)
                    scaleY = 1 - (animationProgress / 10F)

                    val radius = 16.dp * animationProgress
                    shape = RoundedCornerShape(
                        topStart = radius,
                        topEnd = radius,
                        bottomEnd = radius,
                        bottomStart = radius,
                    )
                    clip = true
                },
            color = MaterialTheme.colors.background
        ) {
            window.ProvideWindowInsets {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .withWindowInsetsPadding()
                ) {
                    content()
                    SnackbarUI()
                }
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
fun Window.ProvideWindowInsets(
    content: @Composable () -> Unit
) {
    val initialInset = decorView.rootWindowInsets.toWindowInsets()
    var windowInsets by remember { mutableStateOf(initialInset) }

    CompositionLocalProvider(
        LocalWindowInsets provides windowInsets,
        content = content
    )

    DisposableEffect("ActivityInsets") {
        decorView.setOnApplyWindowInsetsListener { v, insets ->
            windowInsets = insets.toWindowInsets()
            insets.consumeSystemWindowInsets()
        }

        onDispose {
            decorView.setOnApplyWindowInsetsListener(null)
        }
    }
}

val LocalWindowInsets = staticCompositionLocalOf { WindowInsets() }
