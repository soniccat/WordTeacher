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
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kapt)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("com.aglushkov.config-generator")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

javafx {
    version = "19"
    modules("javafx.web", "javafx.swing")
}

kotlin {
    jvmToolchain(17)

    jvm {
        withJava()
        val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        javaPluginConvention.sourceSets.onEach { javaSourceSet ->
            // HACK: adds Dagger generated classes
//            println("java sourceSet:" + javaSourceSet.name + " " + javaSourceSet.java.srcDirs.size)
            javaSourceSet.java.srcDir(project.rootDir.absolutePath + "/shared/build/generated/source/kapt/debug/")
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
                implementation(project(":shared"))
                implementation(compose.desktop.common)
                implementation(compose.desktop.currentOs)
                implementation(libs.decompose)
//                api(compose.runtime)
                implementation(compose.preview)
                implementation(compose.uiTooling)
                implementation(compose.ui)
                implementation(libs.coroutinesSwing)
                implementation(libs.dagger)
                implementation(libs.bouncyCastle)
            }
        }
    }
}

configGenerator {
    configs = listOf(
        com.aglushkov.gradle.ConfigGeneratorItem("Google", "GoogleConfig"),
        com.aglushkov.gradle.ConfigGeneratorItem("GoogleNotPublic", "GoogleConfig"),
        com.aglushkov.gradle.ConfigGeneratorItem("KeyStore", "KeyStoreConfig"),
        com.aglushkov.gradle.ConfigGeneratorItem("KeyStoreNotPublic", "KeyStoreConfig")
    )
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
