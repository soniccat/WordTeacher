buildscript {
    repositories {
        gradlePluginPortal()
        jcenter()
        google()
        mavenCentral()
        maven { url = Deps.mokoPluginsRepo }
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
    mavenCentral()
}

allprojects {
    repositories {
        maven { url = Deps.mokoRepo }
        maven { url = Deps.kotlinxRepo }
    }
}