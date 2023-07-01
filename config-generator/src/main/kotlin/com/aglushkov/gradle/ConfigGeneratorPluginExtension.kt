package com.aglushkov.gradle

data class ConfigGeneratorItem (
    val fileName: String,
    val className: String
)

open class ConfigGeneratorPluginExtension {
    var configs: List<ConfigGeneratorItem> = listOf()
}
