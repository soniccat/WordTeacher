/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resource.generator.desktop

import com.aglushkov.resource.generator.MRGenerator
import com.squareup.kotlinpoet.KModifier
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File

class DesktopMRGenerator(
    generatedDir: File,
    sourceSet: SourceSet,
    mrClassPackage: String,
    generators: List<Generator>
) : MRGenerator(
    generatedDir = generatedDir,
    sourceSet = sourceSet,
    mrClassPackage = mrClassPackage,
    generators = generators
) {
    override fun getMRClassModifiers(): Array<KModifier> = arrayOf(KModifier.ACTUAL)

    override fun apply(generationTask: Task, project: Project) {
        project.tasks.getByName("compileKotlinDesktop").dependsOn(generationTask)
    }
}
