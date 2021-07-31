
buildscript {
    repositories {
        mavenCentral()
        google()

        gradlePluginPortal()
    }
    dependencies {
        //classpath("dev.icerock.moko:resources-generator")
        classpath(":configure-plugin")
    }
}

// TODO: add ktlint

group = "com.aglushkov.wordteacher"
version = "1.0-SNAPSHOT"
