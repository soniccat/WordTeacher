package com.aglushkov.resource.generator

import org.gradle.api.file.FileTree
import java.io.File

data class SourceInfo(
    val generatedDir: File,
    val commonResources: FileTree,
    val mrClassPackage: String,
    val androidRClassPackage: String
)