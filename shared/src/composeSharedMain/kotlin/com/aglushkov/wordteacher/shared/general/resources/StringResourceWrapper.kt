package com.aglushkov.wordteacher.shared.general.resources

import androidx.compose.runtime.Composable
import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import org.jetbrains.compose.resources.stringResource

@Composable
fun StringResourceWrapper.resolve(): String = when(this) {
        is StringResourceWrapper.Raw -> string
        is StringResourceWrapper.String -> stringResource(stringResource)
        is StringResourceWrapper.FormattedString -> stringResource(stringResource, *formatArgs.toTypedArray())
    }