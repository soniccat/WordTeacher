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
    implementation(libs.kotlinxSerialization)
    implementation(libs.mokoResourcesGenerator)
    implementation(libs.mokoResourcesLib)
    implementation(libs.playServicesPlugin)
}
