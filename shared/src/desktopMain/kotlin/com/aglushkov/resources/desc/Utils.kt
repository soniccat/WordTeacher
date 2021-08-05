/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resources.desc

object Utils {
    fun processArgs(args: List<Any>): Array<out Any> {
        return args.map { (it as? StringDesc)?.toResultString() ?: it }.toTypedArray()
    }

    fun getString(resId: String): String {
        // TODO: support getting string by idRes
        return "string with id $resId"
    }

    fun getString(resId: String, vararg formatArgs: Any): String {
        // TODO: support getting string by idRes
        return "varargs string with id $resId"
    }
}
