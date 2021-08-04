/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resources.desc

import android.content.Context
import android.os.Parcelable
import com.aglushkov.resources.StringResource
import kotlinx.parcelize.Parcelize

@Parcelize
actual data class ResourceStringDesc actual constructor(
    val stringRes: StringResource
) : StringDesc, Parcelable {
    override fun toString(context: Context): String {
        return Utils.resourcesForContext(context).getString(stringRes.resourceId)
    }
}
