package com.aglushkov.wordteacher.android_app.general.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.icerock.moko.resources.desc.StringDesc

@Composable
fun StringDesc.resolveString() = toString(LocalContext.current)