/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resources.desc

import com.arkivanov.essenty.parcelable.Parcelable
import com.aglushkov.resources.StringResource

actual data class ResourceStringDesc actual constructor(
    private val stringRes: StringResource
) : StringDesc, Parcelable {
    override fun localized(): String {
        return Utils.localizedString(stringRes)
    }
}
