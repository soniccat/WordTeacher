package com.aglushkov.wordteacher.android_app.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.aglushkov.wordteacher.shared.di.LocalIsDarkTheme
import com.aglushkov.wordteacher.shared.di.LocalIsDebug
import com.aglushkov.wordteacher.shared.general.*

private val DarkColorPalette = darkColors(
    primary = primaryDark,
    primaryVariant = primaryVariantDark,
    secondary = secondaryDark,
    onSecondary = Color.Black,
    background = Color(0xFF2F2F2F),
    surface = Color(0xFF2F2F2F),
)

private val LightColorPalette = lightColors(
    primary = primaryLight,
    primaryVariant = primaryVariantLight,
    secondary = secondaryLight,
    onSecondary = Color.White

    /* Other default colors to override
background = Color.White,
surface = Color.White,
onPrimary = Color.White,
onSecondary = Color.Black,
onBackground = Color.Black,
onSurface = Color.Black,
*/
)

@Composable
fun ComposeAppTheme(
    isDebug: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    CompositionLocalProvider(
        LocalIsDebug provides isDebug,
        LocalIsDarkTheme provides darkTheme,
        LocalAppTypography provides AppTypography().initStylesFromResources(colors)
    ) {
        MaterialTheme(
            colors = colors,
            typography = materialTypography,
            shapes = shapes,
            content = content
        )
    }
}
