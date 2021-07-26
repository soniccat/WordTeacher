
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        // TODO: replace with Deps somehow...
        classpath("com.android.tools.build:gradle:7.0.0-beta05")
    }
}

// TODO: add ktlint

group = "com.aglushkov.wordteacher"
version = "1.0-SNAPSHOT"
