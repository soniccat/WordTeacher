package com.aglushkov.resource

import com.aglushkov.resource.generator.MRGenerator
import com.aglushkov.resource.generator.ResourceGeneratorFeature
import com.aglushkov.resource.generator.SourceInfo
import com.aglushkov.resource.generator.StringsGenerator
import com.aglushkov.resource.generator.android.AndroidMRGenerator
import com.aglushkov.resource.generator.desktop.DesktopMRGenerator
import com.aglushkov.resource.generator.apple.AppleMRGenerator
import com.aglushkov.resource.generator.common.CommonMRGenerator
import com.aglushkov.resource.generator.tasks.GenerateMultiplatformResourcesTask
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ResourcesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val mrExtension =
            target.extensions.create(
                "resourcesPlugin",
                ResourcesPluginExtension::class.java
            )

        target.plugins.withType(KotlinMultiplatformPluginWrapper::class.java) {
            val multiplatformExtension =
                target.extensions.getByType(KotlinMultiplatformExtension::class.java)

            target.plugins.withType(BasePlugin::class.java) {
                val extension = getExtension()

                target.afterEvaluate {
                    configureGenerators(
                        target = target,
                        mrExtension = mrExtension,
                        multiplatformExtension = multiplatformExtension,
                        androidExtension = extension
                    )
                }
            }
        }
    }

    @Suppress("LongMethod")
    private fun configureGenerators(
        target: Project,
        mrExtension: ResourcesPluginExtension,
        multiplatformExtension: KotlinMultiplatformExtension,
        androidExtension: BaseExtension
    ) {
        val androidMainSourceSet =
            androidExtension.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        val desktopMainSourceSet = multiplatformExtension.sourceSets.getByName("desktopMain")
        //multiplatformExtension.targets.findByName("desktopMain").project.exte

        val commonSourceSet = multiplatformExtension.sourceSets.getByName(mrExtension.sourceSetName)
        val commonResources = commonSourceSet.resources

        val manifestFile = androidMainSourceSet.manifest.srcFile
        val androidPackage = getAndroidPackage(manifestFile)

        val generatedDir = File(target.buildDir, "generated/moko")
        val mrClassPackage: String = requireNotNull(mrExtension.multiplatformResourcesPackage) {
            "multiplatformResources.multiplatformResourcesPackage is required!" +
                    " please configure moko-resources plugin correctly."
        }
        val sourceInfo = SourceInfo(
            generatedDir,
            commonResources,
            mrExtension.multiplatformResourcesPackage!!,
            androidPackage
        )
        val iosLocalizationRegion = mrExtension.iosBaseLocalizationRegion
        val features = listOf(
            StringsGenerator.Feature(sourceInfo, iosLocalizationRegion)
        )
        val targets: List<KotlinTarget> = multiplatformExtension.targets.toList()

        val commonGenerationTask = setupCommonGenerator(
            commonSourceSet = commonSourceSet,
            generatedDir = generatedDir,
            mrClassPackage = mrClassPackage,
            features = features,
            target = target
        )
        setupAndroidGenerator(
            targets,
            androidMainSourceSet,
            generatedDir,
            mrClassPackage,
            features,
            target
        )
        setupDesktopGenerator(
            targets,
            desktopMainSourceSet,
            generatedDir,
            mrClassPackage,
            features,
            target
        )
        if (HostManager.hostIsMac) {
            setupAppleGenerator(
                targets,
                generatedDir,
                mrClassPackage,
                features,
                target,
                iosLocalizationRegion
            )
        } else {
            target.logger.warn("MR file generation for iOS is not supported on your system!")
        }

        val generationTasks = target.tasks.filterIsInstance<GenerateMultiplatformResourcesTask>()
        generationTasks.filter { it != commonGenerationTask }
            .forEach { it.dependsOn(commonGenerationTask) }
    }

    private fun setupCommonGenerator(
        commonSourceSet: KotlinSourceSet,
        generatedDir: File,
        mrClassPackage: String,
        features: List<ResourceGeneratorFeature<out MRGenerator.Generator>>,
        target: Project
    ): GenerateMultiplatformResourcesTask {
        val commonGeneratorSourceSet: MRGenerator.SourceSet = createSourceSet(commonSourceSet)
        return CommonMRGenerator(
            generatedDir,
            commonGeneratorSourceSet,
            mrClassPackage,
            generators = features.map { it.createCommonGenerator() }
        ).apply(target)
    }

    @Suppress("LongParameterList")
    private fun setupAndroidGenerator(
        targets: List<KotlinTarget>,
        androidMainSourceSet: AndroidSourceSet,
        generatedDir: File,
        mrClassPackage: String,
        features: List<ResourceGeneratorFeature<out MRGenerator.Generator>>,
        target: Project
    ) {
        val kotlinSourceSets: List<KotlinSourceSet> = targets
            .filterIsInstance<KotlinAndroidTarget>()
            .flatMap { it.compilations }
            .filterNot { it.name.endsWith("Test") } // remove tests compilations
            .map { it.defaultSourceSet }

        val androidSourceSet: MRGenerator.SourceSet =
            createSourceSet(androidMainSourceSet, kotlinSourceSets)
        AndroidMRGenerator(
            generatedDir,
            androidSourceSet,
            mrClassPackage,
            generators = features.map { it.createAndroidGenerator() }
        ).apply(target)
    }

    @Suppress("LongParameterList")
    private fun setupAppleGenerator(
        targets: List<KotlinTarget>,
        generatedDir: File,
        mrClassPackage: String,
        features: List<ResourceGeneratorFeature<out MRGenerator.Generator>>,
        target: Project,
        iosLocalizationRegion: String
    ) {
        val compilations = targets
            .filterIsInstance<KotlinNativeTarget>()
            .filter { it.konanTarget.family.isAppleFamily }
            .map { kotlinNativeTarget ->
                kotlinNativeTarget.compilations
                    .getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            }

        val defSourceSets = compilations.map { it.defaultSourceSet }
        compilations.forEach { compilation ->
            val kss = compilation.defaultSourceSet
            val depend = kss.getDependedFrom(defSourceSets)

            val sourceSet = createSourceSet(depend ?: kss)
            AppleMRGenerator(
                generatedDir,
                sourceSet,
                mrClassPackage,
                generators = features.map { it.createIosGenerator() },
                compilation = compilation,
                baseLocalizationRegion = iosLocalizationRegion
            ).apply(target)
        }
    }

    private fun setupDesktopGenerator(
        targets: List<KotlinTarget>,
        desktopMainSourceSet: KotlinSourceSet,
        generatedDir: File,
        mrClassPackage: String,
        features: List<ResourceGeneratorFeature<out MRGenerator.Generator>>,
        target: Project
    ) {
        val kotlinSourceSets: List<KotlinSourceSet> = targets
            .filterIsInstance<KotlinJvmTarget>()
            .flatMap { it.compilations }
            .filterNot { it.name.endsWith("test") } // remove tests compilations
            .map { it.defaultSourceSet }

        val desktopSourceSet: MRGenerator.SourceSet =
            createSourceSet(desktopMainSourceSet, kotlinSourceSets)
        DesktopMRGenerator(
            generatedDir,
            desktopSourceSet,
            mrClassPackage,
            generators = features.map { it.createDesktopGenerator() }
        ).apply(target)
    }

    private fun getAndroidPackage(manifestFile: File): String {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(manifestFile)

        val manifestNodes = doc.getElementsByTagName("manifest")
        val manifest = manifestNodes.item(0)

        return manifest.attributes.getNamedItem("package").textContent
    }

    private fun createSourceSet(kotlinSourceSet: KotlinSourceSet): MRGenerator.SourceSet {
        return object : MRGenerator.SourceSet {
            override val name: String
                get() = kotlinSourceSet.name

            override fun addSourceDir(directory: File) {
                kotlinSourceSet.kotlin.srcDir(directory)
            }

            override fun addResourcesDir(directory: File) {
                kotlinSourceSet.resources.srcDir(directory)
            }
        }
    }

    private fun createSourceSet(
        androidSourceSet: AndroidSourceSet,
        kotlinSourceSets: List<KotlinSourceSet>
    ): MRGenerator.SourceSet {
        return object : MRGenerator.SourceSet {
            override val name: String
                get() = "android${androidSourceSet.name.capitalize()}"

            override fun addSourceDir(directory: File) {
                kotlinSourceSets.forEach { it.kotlin.srcDir(directory) }
            }

            override fun addResourcesDir(directory: File) {
                androidSourceSet.res.srcDir(directory)
            }
        }
    }

    private fun createSourceSet(
        sourceSet: KotlinSourceSet,
        kotlinSourceSets: List<KotlinSourceSet>
    ): MRGenerator.SourceSet {
        return object : MRGenerator.SourceSet {
            override val name: String
                get() = "desktop${sourceSet.name.capitalize()}"

            override fun addSourceDir(directory: File) {
                kotlinSourceSets.forEach { it.kotlin.srcDir(directory) }
            }

            override fun addResourcesDir(directory: File) {
                sourceSet.resources.srcDir(directory)
            }
        }
    }

    private fun KotlinSourceSet.getDependedFrom(sourceSets: Collection<KotlinSourceSet>): KotlinSourceSet? {
        return sourceSets.firstOrNull { this.dependsOn.contains(it) } ?: this.dependsOn
            .mapNotNull { it.getDependedFrom(sourceSets) }
            .firstOrNull()
    }
}