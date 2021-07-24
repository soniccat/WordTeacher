package com.aglushkov.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

class MyKmmPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply(DependenciesPlugin.Companion.Deps.SqlDelight.plugin)
    }
}