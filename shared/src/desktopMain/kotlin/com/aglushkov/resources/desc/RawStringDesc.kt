/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resources.desc

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
actual data class RawStringDesc actual constructor(
    val string: String
) : StringDesc, Parcelable {
    override fun toString(context: Context): String = string
}
