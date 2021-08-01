
//group = "com.aglushkov.resources-plugin"
//version = "1.0.0"

plugins {
    `kotlin-dsl`
    //id("org.jetbrains.kotlin.jvm") version ("1.5.20")
//    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleKotlinDsl())
    compileOnly(libs.kotlinGradlePlugin)
    compileOnly(libs.androidGradlePlugin)
//    implementation(libs.kotlinPoet)
    implementation(libs.kotlinxSerialization)
//    implementation(libs.apacheCommonsText)
    implementation(libs.kotlinCompilerEmbeddable)

//    api("dev.icerock:mobile-multiplatform:0.12.0")
//    implementation(libs.kotlinClasspath)
//    implementation(libs.androidGradlePlugin)
//    api("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.15.0")
}

gradlePlugin {
    plugins.register("resources") {
        id = "resources"
        implementationClass = "com.aglushkov.resource.MultiplatformResourcesPlugin"
    }
}
