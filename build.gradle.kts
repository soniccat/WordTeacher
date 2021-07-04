buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    dependencies {
        classpath(Deps.Gradle.androidClasspath)
        classpath(Deps.Gradle.kotlinClasspath)
        classpath(Deps.Mp.serializationClasspath)
        classpath(Deps.MokoResources.classpath)
        classpath(Deps.SqlDelight.classpath)

        // Desktop
        // __LATEST_COMPOSE_RELEASE_VERSION__
        classpath("org.jetbrains.compose:compose-gradle-plugin:0.4.0")
//        classpath("com.android.tools.build:gradle:4.0.1")
        // __KOTLIN_COMPOSE_VERSION__
//        classpath(kotlin("gradle-plugin", version = "1.5.10"))
    }
}

// TODO: add ktlint

group = "com.aglushkov.wordteacher"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

subprojects {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}