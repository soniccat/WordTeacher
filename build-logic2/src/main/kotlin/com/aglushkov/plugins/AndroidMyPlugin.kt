package com.aglushkov.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import com.aglushkov.plugins.deps.Deps

class AndroidMyPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply(Deps.SqlDelight.plugin)
    }
}