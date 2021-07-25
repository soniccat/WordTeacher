plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "com.aglushkov.plugins.deps"
version = "SNAPSHOT"

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins.register("deps") {
        id = "deps"
        implementationClass = "com.aglushkov.plugins.deps.DepsPlugin"
    }
}