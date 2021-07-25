package com.aglushkov.plugins.deps

import org.gradle.api.Plugin
import org.gradle.api.Project

class KmmDepsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply(Deps.Mp.serializationPlugin)
        target.pluginManager.apply(Deps.SqlDelight.plugin)
    }
}
