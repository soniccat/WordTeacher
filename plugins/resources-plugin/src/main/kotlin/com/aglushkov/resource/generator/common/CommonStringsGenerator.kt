/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resource.generator.common

import com.aglushkov.resource.generator.StringsGenerator
import com.squareup.kotlinpoet.CodeBlock
import org.gradle.api.file.FileTree

class CommonStringsGenerator(
    stringsFileTree: FileTree
) : StringsGenerator(
    stringsFileTree = stringsFileTree
) {
    override fun getPropertyInitializer(key: String): CodeBlock? = null
}
