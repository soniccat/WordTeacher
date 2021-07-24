import com.aglushkov.plugins.deps.Deps

plugins {
    `kotlin-dsl`// version "1.5.10"
    `java-gradle-plugin`
    id("dependencies")
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
}
dependencies {
    implementation(Deps.SqlDelight.runtime)
    implementation("com.aglushkov.plugins.deps:dependencies:SNAPSHOT")
}

// HACK instead of implementation("com.aglushkov.plugins.deps:dependencies:SNAPSHOT") which doesn' work...
kotlin.sourceSets.getByName("main").kotlin.srcDir("../build-logic/src/main/kotlin")

gradlePlugin {
    plugins {
        create("androidplugin") {
            id = "com.aglushkov.androidplugin"
            implementationClass = "com.aglushkov.plugins.AndroidMyPlugin"
        }
    }
}