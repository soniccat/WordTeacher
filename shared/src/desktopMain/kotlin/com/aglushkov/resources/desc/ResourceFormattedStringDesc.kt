/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resources.desc

import com.aglushkov.resources.StringResource

actual data class ResourceFormattedStringDesc actual constructor(
    val stringRes: StringResource,
    val args: List<Any>
) : StringDesc {
    override fun toResultString(): String {
        @Suppress("SpreadOperator")
        return Utils.getString(
            stringRes.resourceId,
            *Utils.processArgs(args)
        )
    }
}
