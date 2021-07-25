package com.aglushkov.plugins.deps

import com.aglushkov.plugins.deps.Deps
import org.gradle.api.Plugin
import org.gradle.api.Project

class MyKmmLibPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.plugins.apply("org.jetbrains.kotlin.multiplatform")
        target.plugins.apply("com.android.library")
        target.plugins.apply("kotlin-parcelize")
        target.plugins.apply("org.jetbrains.kotlin.native.cocoapods")
        target.plugins.apply(Deps.SqlDelight.plugin)
    }
}
