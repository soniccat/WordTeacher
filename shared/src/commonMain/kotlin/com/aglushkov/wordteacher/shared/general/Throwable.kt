package com.aglushkov.wordteacher.shared.general

import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import wordteacher.shared.generated.resources.Res
import wordteacher.shared.generated.resources.error_default

fun Throwable.toStringResource(): StringResourceWrapper {
    return message?.let {
        StringResourceWrapper.Raw(it)
    } ?: run {
        StringResourceWrapper.String(Res.string.error_default)
    }
}

class StringResourceThrowable(
    val stringResource: StringResourceWrapper,
    cause: Throwable? = null,
): Throwable(null, cause)