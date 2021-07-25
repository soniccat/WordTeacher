
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("deps")
}

// To make it available as direct dependency
//group = "com.aglushkov.plugins.deps"
//version = "SNAPSHOT"

//
//repositories {
//    google()
//    mavenCentral()
//    gradlePluginPortal()
//    mavenLocal()
//}
//
//subprojects {
//    repositories {
//        google()
//        mavenCentral()
//        gradlePluginPortal()
//        mavenLocal()
//    }
//}
//
//allprojects {
//    repositories {
//        google()
//        mavenCentral()
//        gradlePluginPortal()
//        mavenLocal()
//    }
//}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
    implementation(com.aglushkov.plugins.deps.Deps.SqlDelight.classpath)
//    implementation("com.android.tools.build:gradle:7.0.0-beta05")
    /*
    gives
    A problem occurred configuring project ':androidApp'.
> Could not resolve all artifacts for configuration ':androidApp:classpath'.
   > Could not find com.android.tools.build:gradle:7.0.0-beta05.
     Searched in the following locations:
       - https://plugins.gradle.org/m2/com/android/tools/build/gradle/7.0.0-beta05/gradle-7.0.0-beta05.pom
     If the artifact you are trying to retrieve can be found in the repository but without metadata in 'Maven POM' format, you need to adjust the 'metadataSources { ... }' of the repository declaration.
     Required by:
         project :androidApp > project :build-logic
     */
}

kotlin.sourceSets.getByName("main").kotlin.srcDir("../depsSrc/main/kotlin")

gradlePlugin {
    plugins.register("kmmdeps") {
        id = "kmmdeps"
        implementationClass = "com.aglushkov.plugins.deps.MyKmmPlugin"
    }

    plugins.register("kmmlibdeps") {
        id = "kmmlibdeps"
        implementationClass = "com.aglushkov.plugins.deps.MyKmmLibPlugin"
    }
}