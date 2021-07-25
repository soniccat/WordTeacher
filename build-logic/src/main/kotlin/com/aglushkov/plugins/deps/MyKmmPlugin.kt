package com.aglushkov.plugins.deps

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.repositories

class MyKmmPlugin : Plugin<Project> {

    override fun apply(target: Project) {
//        target.dependencies.add("implementation", Deps.SqlDelight.classpath)
//        target.plugins.apply(Deps.SqlDelight.plugin)
        target.pluginManager.apply(Deps.SqlDelight.plugin)
        //target.dependencies.add("implementation", Deps.SqlDelight.classpath)
    }
}
