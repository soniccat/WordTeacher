package com.aglushkov.wordteacher.androidApp.general.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.aglushkov.resources.desc.StringDesc

@Composable
fun StringDesc.resolveString() = toString(LocalContext.current)