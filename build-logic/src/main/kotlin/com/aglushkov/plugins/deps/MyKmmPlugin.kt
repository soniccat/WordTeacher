package com.aglushkov.plugins.deps

import org.gradle.api.Plugin
import org.gradle.api.Project

class MyKmmPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply(Deps.SqlDelight.plugin)
    }
}