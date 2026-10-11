package com.aglushkov.wordteacher.shared.general.resource

import org.jetbrains.compose.resources.StringResource

sealed interface StringResourceWrapper {
    data class Raw(val string: kotlin.String): StringResourceWrapper
    data class String(val stringResource: StringResource): StringResourceWrapper
    data class FormattedString(val stringResource: StringResource, val formatArgs: List<Any>): StringResourceWrapper
}

