/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resource.generator

import com.squareup.kotlinpoet.TypeSpec

interface ObjectBodyExtendable {
    fun extendObjectBody(classBuilder: TypeSpec.Builder)
}
