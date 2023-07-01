package com.aglushkov.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File
import java.util.*

class ConfigGeneratorPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val pluginExtension: ConfigGeneratorPluginExtension = target.extensions.create(
            "configGenerator",
            ConfigGeneratorPluginExtension::class.java
        )

        target.afterEvaluate {
            generateFiles(target, pluginExtension)
        }
    }

    private fun generateFiles(
        target: Project,
        pluginExtension: ConfigGeneratorPluginExtension
    ) {
        val kmmExtension = target.extensions.getByType(KotlinMultiplatformExtension::class.java)
        kmmExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME).kotlin.srcDir(
            configsSourceSetPath(target)
        )

        pluginExtension.configs.onEach { config ->
            val filePath =
                target.projectDir.path + "/" + config.fileName + ".properties"
            val file = File(filePath)
            if (file.exists()) {
                val properties = file.inputStream().use { inputStream ->
                    Properties().apply {
                        load(inputStream)
                    }
                }

                createConfigClass(target, config.className, properties)
            }
        }
    }

    private fun createConfigClass(target: Project, className: String, properties: Properties) {
        val companion = com.squareup.kotlinpoet.TypeSpec.companionObjectBuilder().apply {
            properties.onEach { propEntry ->
                addProperty(
                    com.squareup.kotlinpoet.PropertySpec.Companion.builder(propEntry.key as String, String::class)
                        .initializer("%S", propEntry.value as String)
                        .build()
                )
            }
        }.build()
        val cl = com.squareup.kotlinpoet.TypeSpec.classBuilder(className)
            .addType(companion)
            .build()
        val fileSpec = com.squareup.kotlinpoet.FileSpec.builder("com.aglushkov.wordteacher.desktopapp.configs", className)
            .addType(cl)
            .build()
        fileSpec.writeTo(target.file(configsSourceSetPath(target)))
    }

    private fun configsSourceSetPath(target: Project): String {
        return target.buildDir.path + "/generated/configs/code/main"
    }
}
