
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
    implementation(com.aglushkov.plugins.deps2.Deps.SqlDelight.classpath)
    //implementation("com.squareup.sqldelight:gradle-plugin:1.5.0")
//    implementation(com.aglushkov.plugins.deps.Deps.SqlDelight.classpath)
}

gradlePlugin {
//    plugins {
//        create("dependencies") {
//            id = "dependencies"
//            implementationClass = "com.aglushkov.plugins.deps.DependenciesPlugin"
//        }
    plugins.register("kmmdeps") {
        id = "kmmdeps"
        implementationClass = "com.aglushkov.plugins.deps.MyKmmPlugin"
    }
}