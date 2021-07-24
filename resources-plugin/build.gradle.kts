
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }

    dependencies {
        classpath(":build-logic")
    }
}

group = "com.aglushkov.resources-plugin"
version = "1.0.0"

plugins {
    id("org.jetbrains.kotlin.jvm") version ("1.5.20")
    id("deps")
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleKotlinDsl())
    compileOnly(Deps_gradle.MyDeps.Gradle.kotlinClasspath)
    compileOnly(Deps_gradle.MyDeps.Gradle.androidClasspath)
//    implementation(libs.kotlinPoet)
//    implementation(libs.kotlinxSerialization)
//    implementation(libs.apacheCommonsText)
//    implementation(libs.kotlinCompilerEmbeddable)
}
