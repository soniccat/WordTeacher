/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resources.desc

import com.aglushkov.resources.StringResource
import com.arkivanov.decompose.statekeeper.Parcelable

expect class ResourceStringDesc(stringRes: StringResource) : StringDesc, Parcelable

@Suppress("FunctionName")
fun StringDesc.Companion.Resource(stringRes: StringResource) = ResourceStringDesc(stringRes)
