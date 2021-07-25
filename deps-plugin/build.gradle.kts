
plugins {
    `kotlin-dsl`
}

// To make it available as direct dependency
group = "com.aglushkov.plugins.deps2"
version = "SNAPSHOT"

repositories {
    mavenCentral()
}

//kotlin.sourceSets.getByName("main").kotlin.srcDir("../depsSrc/main/kotlin")

//dependencies {
//    implementation("com.squareup.sqldelight:gradle-plugin:1.5.0")
//    implementation(com.aglushkov.plugins.deps2.Deps.SqlDelight.classpath)
//}

gradlePlugin {
//    plugins {
//        create("dependencies") {
//            id = "dependencies"
//            implementationClass = "com.aglushkov.plugins.deps2.DependenciesPlugin"
//        }
    plugins.register("deps") {
        id = "deps"
        implementationClass = "com.aglushkov.plugins.deps2.DepsPlugin"
    }
}