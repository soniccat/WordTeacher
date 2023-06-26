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
        javaPluginConvention.sourceSets.all { javaSourceSet ->
            // HACK: adds Dagger generated classes
//            println("java sourceSet:" + javaSourceSet.name + " " + javaSourceSet.java.srcDirs.size)
            javaSourceSet.java.srcDir(project.rootDir.absolutePath + "/shared/build/generated/source/kapt/main/")
//            javaSourceSet.java.srcDirs.onEach { dir ->
//                println("srcDir file:" + dir.absolutePath)
//            }
//            javaSourceSet.allJava.onEach { file ->
//                println("java file:" + file.absolutePath)
//            }
            true
        }
    }

    sourceSets {
        val jvmMain by getting {
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

tasks.create("generateAppConfig") {
    val propFiles = listOf("Google")

    doLast {
        propFiles.onEach { fileName ->
            val filePath = projectDir.path + "/" + fileName + ".properties"
            println("Reading property file: $filePath")

            val properties = File(filePath).inputStream().use { inputStream ->
                Properties().apply {
                    load(inputStream)
                }
            }

            createConfigClass(fileName, properties)
        }
    }
}

// To generate AppConfig.kt
fun createConfigClass(prefix: String, properties: Properties) {
    val companion = com.squareup.kotlinpoet.TypeSpec.companionObjectBuilder()
        .addProperty(
            com.squareup.kotlinpoet.PropertySpec.Companion.builder("testProp", String::class)
                .initializer("%S", "defaultValue")
                .build()
        )
        .build()
    val className = prefix + "Config"
    val cl = com.squareup.kotlinpoet.TypeSpec.classBuilder(className)
        .addType(companion)
        .build()
    val file = com.squareup.kotlinpoet.FileSpec.builder("com.aglushkov.wordteacher.desktopapp", className)
        .addType(cl)
        .build()

    println("hello from test")
    file.writeTo(System.out)
}


tasks.named("jvmProcessResources") {
    dependsOn(":desktopApp:generateAppConfig")
}
