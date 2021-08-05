/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resources.desc

actual data class CompositionStringDesc actual constructor(
    val args: Iterable<StringDesc>,
    val separator: String?
) : StringDesc {
    override fun toResultString(): String {
        return StringBuilder().apply {
            args.forEachIndexed { index, stringDesc ->
                if (index != 0 && separator != null) {
                    append(separator)
                }
                append(stringDesc.toResultString())
            }
        }.toString()
    }
}
