package com.aglushkov.wordteacher.shared.general

import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.RawStringDesc
import dev.icerock.moko.resources.desc.ResourceStringDesc
import dev.icerock.moko.resources.desc.StringDesc

fun Throwable.toStringDesc(): StringDesc {
    return message?.let {
        RawStringDesc(it)
    } ?: run {
        ResourceStringDesc(MR.strings.error_default)
    }
}
