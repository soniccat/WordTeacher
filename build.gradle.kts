buildscript {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    dependencies {
        classpath(":configure-plugin")
        classpath(":resources-plugin")
        classpath("org.jetbrains.compose:compose-gradle-plugin:0.4.0")
    }
}

// TODO: add ktlint

group = "com.aglushkov.wordteacher"
version = "1.0-SNAPSHOT"
