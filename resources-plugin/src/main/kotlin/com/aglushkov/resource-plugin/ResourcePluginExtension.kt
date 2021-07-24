package com.aglushkov.resource

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

open class ResourcePluginExtension {
    var multiplatformResourcesPackage: String? = null
    var multiplatformResourcesSourceSet: String? = null
    var iosBaseLocalizationRegion: String = "en"
    val sourceSetName: String get() = multiplatformResourcesSourceSet ?: KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
    var disableStaticFrameworkWarning = false
}