package com.aglushkov.wordteacher.shared.general

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.activity.BackEventCompat
import androidx.activity.SystemBarStyle
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.primarySurface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.toColor
import androidx.core.graphics.toColorLong
import androidx.core.view.WindowInsetsControllerCompat
import com.aglushkov.wordteacher.shared.general.views.pxToDp
import com.aglushkov.wordteacher.shared.general.views.windowInsetsVerticalPadding
import com.aglushkov.wordteacher.shared.general.views.windowInsetsVerticalPaddingWithIME
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.color.MaterialColors

@Composable
actual fun CustomDialogUI(
    onDismissRequest: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val window = findDialogWindow() ?: throw Resources.NotFoundException("Window isn't found")
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        window.setDimAmount(0.5f)

        val view = window.decorView
        val statusBarIsDark = (view.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = !statusBarIsDark
        }

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = -animationProgress * 5.dp.toPx()
                    translationX = animationProgress * 25.dp.toPx()
                    alpha = 1 - animationProgress
                    scaleX = 1 - (animationProgress / 10F)
                    scaleY = 1 - (animationProgress / 10F)

                    val radius = 16.dp.toPx() * animationProgress
                    shape = RoundedCornerShape(
                        topStart = radius,
                        topEnd = radius,
                        bottomEnd = radius,
                        bottomStart = radius,
                    )
                    clip = true
                }
                .background(
                    if (MaterialTheme.colors.isLight) {
                        MaterialTheme.colors.primarySurface
                    } else {
                        // hack to make the color above the appbar equal to the appbar background
                        // changing DarkColorPalette.surface makes the appbar background lighter
                        // need to investigate... on the same time in the main activity everything
                        // works as expected
                        DarkWindowBackground
                    }
                )
                .windowInsetsVerticalPaddingWithIME(),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                content()
                SnackbarUI()
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

public val DarkWindowBackground = Color(0xFF434343)