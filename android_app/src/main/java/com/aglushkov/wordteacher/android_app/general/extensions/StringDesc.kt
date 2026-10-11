package com.aglushkov.wordteacher.android_app.general.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.StringResource

@Composable
fun StringResource.resolveString() = toString(LocalContext.current)