package com.aglushkov.wordteacher.shared.general.resource

import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc

fun Resource<*>?.getErrorString(hasConnection: Boolean, hasResponse: Boolean): StringDesc? {
    if (this is Resource.Error) {
        if (!hasConnection) {
            return StringDesc.Resource(MR.strings.error_no_connection)
        } else if (!hasResponse) {
            return StringDesc.Resource(MR.strings.error_bad_connection)
        } else {
            return StringDesc.Resource(MR.strings.error_bad_response)
        }
    }

    return null
}