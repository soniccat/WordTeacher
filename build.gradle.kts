buildscript {
    repositories {
        gradlePluginPortal()
        jcenter()
        google()
        mavenCentral()
        maven { url = Deps.mokoPluginsRepo }
    }

    dependencies {
        classpath(Deps.Gradle.androidGradlePlugin)
        classpath(Deps.Gradle.kotlinGradlePlugin)
        classpath(Deps.Mp.serialization)
        classpath(Deps.MokoResources.classpath)
    }
}

// TODO: add ktlint

group = "com.aglushkov.wordteacher"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

allprojects {
    repositories {
        maven { url = Deps.mokoRepo }
        maven { url = Deps.kotlinxRepo }
    }
}