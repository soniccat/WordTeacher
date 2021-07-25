
plugins {
    `kotlin-dsl`
    id("deps")
}

// To make it available as direct dependency
group = "com.aglushkov.plugins.deps"
version = "SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin.sourceSets.getByName("main").kotlin.srcDir("../depsSrc/main/kotlin")

dependencies {
    implementation(com.aglushkov.plugins.deps.Deps.SqlDelight.classpath)
}

gradlePlugin {
    plugins.register("kmmdeps") {
        id = "kmmdeps"
        implementationClass = "com.aglushkov.plugins.deps.MyKmmPlugin"
    }
}