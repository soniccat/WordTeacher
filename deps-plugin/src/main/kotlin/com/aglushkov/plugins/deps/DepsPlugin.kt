package com.aglushkov.plugins.deps

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.repositories

class DepsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
    }
}
