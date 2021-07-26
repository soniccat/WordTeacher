
plugins {
    `kotlin-dsl`
}

// To make it available as direct dependency
group = "com.aglushkov.plugins.deps"
version = "SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin.sourceSets.getByName("main").kotlin.srcDir("../depsSrc/main/kotlin")

gradlePlugin {
    // HACK: The plugin is here to be able to access Deps inside of other kts files...
    plugins.register("deps") {
        id = "deps"
        implementationClass = "com.aglushkov.plugins.deps.DepsPlugin"
    }
}
