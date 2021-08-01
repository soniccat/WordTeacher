package com.aglushkov.resource

import com.android.build.gradle.BasePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class MultiplatformResourcesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val mrExtension =
            target.extensions.create(
                "resourcesPlugin",
                ResourcePluginExtension::class.java
            )

        target.plugins.withType(KotlinMultiplatformPluginWrapper::class.java) {
            val multiplatformExtension =
                target.extensions.getByType(KotlinMultiplatformExtension::class.java)

            target.plugins.withType(BasePlugin::class.java) {
//                val extension = it.getExtension()
//
//                target.afterEvaluate {
//                    configureGenerators(
//                        target = target,
//                        mrExtension = mrExtension,
//                        multiplatformExtension = multiplatformExtension,
//                        androidExtension = extension
//                    )
//                }
            }
        }
    }
}