/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resource.generator.apple

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.aglushkov.resource.generator.MRGenerator
import com.aglushkov.resource.ResourcesPluginExtension
import com.aglushkov.resource.generator.tasks.CopyFrameworkResourcesToAppTask
import com.aglushkov.resource.generator.tasks.CopyFrameworkResourcesToAppEntryPointTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.library.impl.KotlinLibraryLayoutImpl
import java.io.File
import java.io.InputStream
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

@Suppress("TooManyFunctions")
class AppleMRGenerator(
    generatedDir: File,
    sourceSet: SourceSet,
    mrClassPackage: String,
    generators: List<Generator>,
    private val compilation: AbstractKotlinNativeCompilation,
    private val baseLocalizationRegion: String
) : MRGenerator(
    generatedDir = generatedDir,
    sourceSet = sourceSet,
    mrClassPackage = mrClassPackage,
    generators = generators
) {
    private val bundleClassName =
        ClassName("platform.Foundation", "NSBundle")
    private val bundleIdentifier = "$mrClassPackage.MR"

    private var assetsDirectory: File? = null

    override fun getMRClassModifiers(): Array<KModifier> = arrayOf(KModifier.ACTUAL)

    override fun processMRClass(mrClass: TypeSpec.Builder) {
        super.processMRClass(mrClass)

        mrClass.addProperty(
            PropertySpec.builder(
                BUNDLE_PROPERTY_NAME,
                bundleClassName,
                KModifier.PRIVATE
            )
                .delegate(CodeBlock.of("lazy { NSBundle.loadableBundle(\"$bundleIdentifier\") }"))
                .build()
        )
    }

    override fun getImports(): List<ClassName> = listOf(
        bundleClassName,
        ClassName("com.aglushkov.resources.utils", "loadableBundle")
    )

    override fun apply(generationTask: Task, project: Project) {
        setupKLibResources(generationTask)
        setupFrameworkResources()
        setupTestsResources()
    }

    override fun beforeMRGeneration() {
        assetsDirectory = File(resourcesGenerationDir, ASSETS_DIR_NAME).apply {
            mkdirs()
        }
    }

    private fun setupKLibResources(generationTask: Task) {
        val compileTask: KotlinNativeCompile = compilation.compileKotlinTask
        compileTask.dependsOn(generationTask)

        // lambda will broke gradle UP-TO-DATE mark!
        @Suppress("ObjectLiteralToLambda")
        compileTask.doLast(object : Action<Task> {
            override fun execute(task: Task) {
                task as KotlinNativeCompile

                val klibFile = task.outputFile.get()
                val repackDir = File(klibFile.parent, klibFile.nameWithoutExtension)
                val defaultDir = File(repackDir, "default")
                val resRepackDir = File(defaultDir, "resources")

                unzipTo(zipFile = klibFile, outputDirectory = repackDir)

                val manifestFile = File(defaultDir, "manifest")
                val manifest = Properties()
                manifest.load(manifestFile.inputStream())

                val uniqueName = manifest["unique_name"] as String

                val loadableBundle = LoadableBundle(
                    directory = resRepackDir,
                    bundleName = uniqueName,
                    developmentRegion = baseLocalizationRegion,
                    identifier = bundleIdentifier
                )
                loadableBundle.write()

                assetsDirectory?.let { assetsDir ->
                    val process = Runtime.getRuntime().exec(
                        "xcrun actool Assets.xcassets --compile . --platform iphoneos --minimum-deployment-target 9.0",
                        emptyArray(),
                        assetsDir.parentFile
                    )
                    val errors = process.errorStream.bufferedReader().readText()
                    val input = process.inputStream.bufferedReader().readText()
                    val result = process.waitFor()
                    if (result != 0) {
                        println("can't compile assets - $result")
                        println(input)
                        println(errors)
                    } else {
                        assetsDir.deleteRecursively()
                    }
                }

                resourcesGenerationDir.copyRecursively(
                    loadableBundle.resourcesDir,
                    overwrite = true
                )

                val repackKonan = org.jetbrains.kotlin.konan.file.File(repackDir.path)
                val klibKonan = org.jetbrains.kotlin.konan.file.File(klibFile.path)

                klibFile.delete()
                repackKonan.zipDirAs(klibKonan)

                repackDir.deleteRecursively()
            }
        })
    }

    private fun setupFrameworkResources() {
        val kotlinNativeTarget = compilation.target as KotlinNativeTarget
        val project = kotlinNativeTarget.project

        kotlinNativeTarget.binaries
            .matching { it is Framework && it.compilation == compilation }
            .configureEach {
                val framework = this as Framework

                val linkTask = framework.linkTask

                // lambda will broke gradle UP-TO-DATE mark!
                @Suppress("ObjectLiteralToLambda")
                linkTask.doLast(object : Action<Task> {
                    override fun execute(task: Task) {
                        task as KotlinNativeLink

                        copyKlibsResourcesIntoFramework(task)
                    }
                })

                if (framework.isStatic) {
                    val resourcesExtension =
                        project.extensions.getByType(ResourcesPluginExtension::class.java)
                    if (resourcesExtension.disableStaticFrameworkWarning.not()) {
                        project.logger.warn(
                            """
$linkTask produces static framework, Xcode should have Build Phase with copyFrameworkResourcesToApp gradle task call. Please read readme on https://github.com/icerockdev/moko-resources
"""
                        )
                    }
                    createCopyFrameworkResourcesTask(linkTask)
                }
            }
    }

    private fun copyKlibsResourcesIntoFramework(linkTask: KotlinNativeLink) {
        val project = linkTask.project
        val framework = linkTask.binary as Framework

        copyResourcesFromLibraries(
            linkTask = linkTask,
            project = project,
            outputDir = framework.outputFile
        )
    }

    private fun copyResourcesFromLibraries(
        linkTask: KotlinNativeLink,
        project: Project,
        outputDir: File
    ) {
        linkTask.libraries
            .plus(linkTask.intermediateLibrary.get())
            .filter { it.extension == "klib" }
            .forEach {
                project.logger.info("copy resources from $it into $outputDir")
                val klibKonan = org.jetbrains.kotlin.konan.file.File(it.path)
                val klib = KotlinLibraryLayoutImpl(klib = klibKonan, component = "default")
                val layout = klib.extractingToTemp

                File(layout.resourcesDir.path).copyRecursively(
                    target = outputDir,
                    overwrite = true
                )
            }
    }

    private fun createCopyFrameworkResourcesTask(linkTask: KotlinNativeLink) {
        val framework = linkTask.binary as Framework
        val project = linkTask.project
        val taskName = linkTask.name.replace("link", "copyResources")

        val copyTask = project.tasks.create(taskName, CopyFrameworkResourcesToAppTask::class.java) {
            this.framework = framework
        }
        copyTask.dependsOn(linkTask)

        val xcodeTask = project.tasks.maybeCreate(
            "copyFrameworkResourcesToApp",
            CopyFrameworkResourcesToAppEntryPointTask::class.java
        )

        if (framework.target.konanTarget == xcodeTask.konanTarget &&
            framework.buildType.getName() == xcodeTask.configuration
        ) {
            xcodeTask.dependsOn(copyTask)
        }
    }

    private fun setupTestsResources() {
        val kotlinNativeTarget = compilation.target as KotlinNativeTarget
        val project = kotlinNativeTarget.project

        kotlinNativeTarget.binaries
            .matching { it is TestExecutable && it.compilation.associateWith.contains(compilation) }
            .configureEach {
                val executable = this as TestExecutable

                val linkTask = executable.linkTask

                // lambda will broke gradle UP-TO-DATE mark!
                @Suppress("ObjectLiteralToLambda")
                linkTask.doLast(object : Action<Task> {
                    override fun execute(task: Task) {
                        task as KotlinNativeLink
                        copyResourcesFromLibraries(
                            linkTask = task,
                            project = project,
                            outputDir = executable.outputDirectory
                        )
                    }
                })
            }
    }

    private fun unzipTo(outputDirectory: File, zipFile: File) {
        ZipFile(zipFile).use { zip ->
            val outputDirectoryCanonicalPath = outputDirectory.canonicalPath
            for (entry in zip.entries()) {
                unzipEntryTo(outputDirectory, outputDirectoryCanonicalPath, zip, entry)
            }
        }
    }

    private fun unzipEntryTo(
        outputDirectory: File,
        outputDirectoryCanonicalPath: String,
        zip: ZipFile,
        entry: ZipEntry
    ) {
        val output = outputDirectory.resolve(entry.name)
        if (!output.canonicalPath.startsWith(outputDirectoryCanonicalPath)) {
            throw ZipException("Zip entry '${entry.name}' is outside of the output directory")
        }
        if (entry.isDirectory) {
            output.mkdirs()
        } else {
            output.parentFile.mkdirs()
            zip.getInputStream(entry).use { it.copyTo(output) }
        }
    }

    private fun InputStream.copyTo(file: File): Long =
        file.outputStream().use { copyTo(it) }

    companion object {
        const val BUNDLE_PROPERTY_NAME = "bundle"
        const val ASSETS_DIR_NAME = "Assets.xcassets"
    }
}
