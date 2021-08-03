package com.aglushkov.wordteacher.shared.general.resource

import com.aglushkov.resources.desc.Resource
import com.aglushkov.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.res.MR

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