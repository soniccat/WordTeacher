/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resources.desc

import java.util.Locale

actual interface StringDesc {
    fun toResultString(): String

    actual sealed class LocaleType {
        actual object System : LocaleType() {
            override val systemLocale: Locale? = null
        }

        actual class Custom actual constructor(
            locale: String
        ) : LocaleType() {
            override val systemLocale: Locale = Locale(locale)
        }

        abstract val systemLocale: Locale?
    }

    actual companion object {
        actual var localeType: LocaleType = LocaleType.System
    }
}
