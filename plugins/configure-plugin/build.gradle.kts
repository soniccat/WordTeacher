
plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.androidGradlePlugin)
    implementation(libs.kotlinGradlePlugin)
//    implementation(com.aglushkov.plugins.deps.Deps.Gradle.kotlinClasspath)
    implementation(libs.sqlDelight)
    implementation(libs.kotlinxSerialization)
    // TODO: return this:
    /*
    Not it gives that:
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

// To be able to access Deps in MyKmmLibPlugin
//kotlin.sourceSets.getByName("main").kotlin.srcDir("../depsSrc/main/kotlin")

//gradlePlugin {
//    plugins.register("androidapp") {
//        id = "androidapp"
//        implementationClass = "com.aglushkov.plugins.deps.AndroidAppPlugin"
//    }
//
//    plugins.register("kmmlib") {
//        id = "kmmlib"
//        implementationClass = "com.aglushkov.plugins.deps.KmmLibPlugin"
//    }
//}
