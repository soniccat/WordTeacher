
plugins {
    `kotlin-dsl`
}

// To make it available as direct dependency
group = "com.aglushkov.plugins.deps2"
version = "SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin.sourceSets.getByName("main").kotlin.srcDir("../depsSrc/main/kotlin")

gradlePlugin {
    plugins.register("deps") {
        id = "deps"
        implementationClass = "com.aglushkov.plugins.deps2.DepsPlugin"
    }
}