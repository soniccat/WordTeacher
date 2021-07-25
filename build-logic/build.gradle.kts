import com.aglushkov.plugins.deps.Deps

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("deps")
}

group = "com.aglushkov.plugins.deps"
version = "SNAPSHOT"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
}

dependencies {
    implementation(Deps.Gradle.androidClasspath)
}

gradlePlugin {
    plugins.register("kmmdeps") {
        id = "kmmdeps"
        implementationClass = "com.aglushkov.plugins.deps.KmmDepsPlugin"
    }
}