/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resources.desc

import com.arkivanov.essenty.parcelable.Parcelable

expect class RawStringDesc(string: String) : StringDesc, Parcelable

@Suppress("FunctionName")
fun StringDesc.Companion.Raw(string: String) = RawStringDesc(string)
