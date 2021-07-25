import com.aglushkov.plugins.deps.Deps

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("deps")
}

group = "com.aglushkov.plugins.deps"
version = "SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(Deps.Gradle.androidClasspath)
}

gradlePlugin {
    plugins.register("deps") {
        id = "deps"
        implementationClass = "com.aglushkov.plugins.deps.DepsPlugin"
    }
    plugins.register("kmmdeps") {
        id = "kmmdeps"
        implementationClass = "com.aglushkov.plugins.deps.KmmDepsPlugin"
    }
}