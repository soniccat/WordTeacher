package com.aglushkov.wordteacher.shared.general.resource

import wordteacher.shared.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import wordteacher.shared.generated.resources.error_bad_connection
import wordteacher.shared.generated.resources.error_bad_response
import wordteacher.shared.generated.resources.error_no_connection

fun Resource<*>?.getErrorString(hasConnection: Boolean, hasResponse: Boolean): StringResourceWrapper? {
    if (this is Resource.Error) {
        if (!hasConnection) {
            return StringResourceWrapper.String(Res.string.error_no_connection)
        } else if (!hasResponse) {
            return StringResourceWrapper.String(Res.string.error_bad_connection)
        } else {
            return StringResourceWrapper.String(Res.string.error_bad_response)
        }
    }

    return null
}