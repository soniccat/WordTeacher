/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resources.desc

import com.aglushkov.resources.StringResource
import com.arkivanov.essenty.parcelable.Parcelable

actual data class ResourceStringDesc actual constructor(
    val stringRes: StringResource
) : StringDesc, Parcelable {
    override fun toResultString(): String {
        return Utils.getString(stringRes.resourceId)
    }
}
