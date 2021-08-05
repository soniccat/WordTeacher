/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resources.desc

import com.arkivanov.essenty.parcelable.Parcelable

actual data class RawStringDesc actual constructor(
    val string: String
) : StringDesc, Parcelable {
    override fun toResultString(): String = string
}
