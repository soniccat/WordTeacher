import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.gradle.api.NamedDomainObjectContainer
import java.util.Properties

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

plugins {
    kotlin("multiplatform")
    id("kotlin-kapt")
    id("org.jetbrains.compose")
    id("org.openjfx.javafxplugin") version "0.0.13"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

javafx {
    version = "19"
    modules("javafx.web", "javafx.swing")
}

kotlin {
    jvm {
        withJava()
        val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        javaPluginConvention.sourceSets.onEach { javaSourceSet ->
            // HACK: adds Dagger generated classes
//            println("java sourceSet:" + javaSourceSet.name + " " + javaSourceSet.java.srcDirs.size)
            javaSourceSet.java.srcDir(project.rootDir.absolutePath + "/shared/build/generated/source/kapt/main/")
//            javaSourceSet.java.srcDirs.onEach { dir ->
//                println("srcDir file:" + dir.absolutePath)
//            }
//            javaSourceSet.allJava.onEach { file ->
//                println("java file:" + file.absolutePath)
//            }
        }
    }

    sourceSets {
        getByName("jvmMain") {
            dependencies {
                compileOnly("com.squareup:kotlinpoet:1.14.2")

                implementation(project(":shared"))
                implementation(compose.desktop.common)
                implementation(compose.desktop.currentOs)
                implementation(libs.decompose)
                implementation(compose.runtime)
                implementation(compose.preview)
                implementation(compose.uiTooling)
                implementation(compose.ui)
                implementation(libs.coroutinesSwing)
                implementation(libs.dagger)
            }
        }
    }
}

dependencies {
    add("kapt", libs.daggerCompiler)
}

compose.desktop {
    application {
        mainClass = "com.aglushkov.wordteacher.desktopapp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "WordTeacher"
            packageVersion = "1.0.0"

            modules("java.sql")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

data class Config (
    val fileName: String,
    val className: String
)

tasks.create("generateAppConfig") {
    val propFiles = listOf(
        Config("Google", "GoogleConfig"),
        Config("GoogleAuth", "GoogleConfig")
    )
    project.kotlin.sourceSets["jvmMain"].kotlin.srcDir(configsSourceSetPath())

    doLast {
        propFiles.onEach { config ->
            val filePath = projectDir.path + "/" + config.fileName + ".properties"
            val file = File(filePath)
            if (file.exists()) {
                val properties = file.inputStream().use { inputStream ->
                    Properties().apply {
                        load(inputStream)
                    }
                }

                createConfigClass(config.className, properties)
            }
        }
    }
}

fun createConfigClass(className: String, properties: Properties) {
    val companion = com.squareup.kotlinpoet.TypeSpec.companionObjectBuilder().apply {
        properties.onEach { propEntry ->
            addProperty(
                com.squareup.kotlinpoet.PropertySpec.Companion.builder(propEntry.key as String, String::class)
                    .initializer("%S", propEntry.value as String)
                    .build()
            )
        }
    }.build()
    val cl = com.squareup.kotlinpoet.TypeSpec.classBuilder(className)
        .addType(companion)
        .build()
    val fileSpec = com.squareup.kotlinpoet.FileSpec.builder("com.aglushkov.wordteacher.desktopapp.configs", className)
        .addType(cl)
        .build()
    //println("hello from test :" + project.buildDir.path + "/generated/source/configs/main/com/aglushkov/wordteacher/desktopapp/config/")
    fileSpec.writeTo(file(configsSourceSetPath()))
}

fun configsSourceSetPath(): String {
    return project.buildDir.path + "/generated/source/configs/main"
}

tasks.named("jvmProcessResources") {
    dependsOn(":desktopApp:generateAppConfig")
}
