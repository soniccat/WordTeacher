/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resources.desc

import com.aglushkov.resources.StringResource

actual data class ResourceFormattedStringDesc actual constructor(
    val stringRes: StringResource,
    val args: List<Any>
) : StringDesc {
    override fun localized(): String {
        return Utils.stringWithFormat(Utils.localizedString(stringRes), Utils.processArgs(args))
    }
}
