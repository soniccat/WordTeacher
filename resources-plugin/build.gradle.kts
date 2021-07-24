//import com.aglushkov.plugins.DependenciesPlugin.Companion.Deps

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }

    dependencies {
//        classpath(":build-logic")
    }
}

group = "com.aglushkov.resources-plugin"
version = "1.0.0"

plugins {
//    id("org.jetbrains.kotlin.jvm") version ("1.5.20")
    id("dependencies")
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
//    implementation(gradleKotlinDsl())
//    implementation(Deps.Gradle.kotlinClasspath)
//    compileOnly(Deps.Gradle.androidClasspath)
//    implementation(libs.kotlinPoet)
//    implementation(libs.kotlinxSerialization)
//    implementation(libs.apacheCommonsText)
//    implementation(libs.kotlinCompilerEmbeddable)
}
