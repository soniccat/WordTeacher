/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resource.generator

interface ResourceGeneratorFeature<T : MRGenerator.Generator> {
    fun createCommonGenerator(): T
    fun createIosGenerator(): T
    fun createAndroidGenerator(): T
    fun createDesktopGenerator(): T
}
