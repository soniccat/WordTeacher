
plugins {
    `kotlin-dsl`
    //id("org.jetbrains.compose")
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(libs.androidGradlePlugin)
    implementation(libs.androidComposeDesktop)
    implementation(libs.kotlinGradlePlugin)
    implementation(libs.sqlDelight)
//    implementation(libs.kotlinxSerialization)
    implementation(libs.mokoResourcesGenerator)
    implementation(libs.mokoResourcesLib)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

gradlePlugin {
    plugins.register("resources") {
        id = "resources"
        implementationClass = "com.aglushkov.resource.ResourcesPlugin"
    }
}