import com.aglushkov.plugins.DependenciesPlugin.Companion.Deps

plugins {
    `kotlin-dsl`
    id("com.aglushkov.plugins")
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
}

dependencies {
    implementation(Deps.SqlDelight)
}

//gradlePlugin {
//    plugins {
//        create("androiddeps") {
//            id = "com.aglushkov.plugins"
//            implementationClass = "com.aglushkov.plugins.DependenciesPlugin"
//        }
//    }
//}