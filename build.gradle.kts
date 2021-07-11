buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }

    dependencies {
        classpath(Deps.Gradle.androidClasspath)
        classpath(Deps.Gradle.kotlinClasspath)
        classpath(Deps.Mp.serializationClasspath)
        classpath(Deps.MokoResources.classpath)
        classpath(Deps.SqlDelight.classpath)
    }
}

// TODO: add ktlint

group = "com.aglushkov.wordteacher"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
}

subprojects {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}