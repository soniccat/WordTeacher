
plugins {
    `kotlin-dsl`
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
    implementation(libs.kotlinPoet)
    implementation(libs.kotlinxSerialization)
    implementation(libs.apacheCommonsText)
    implementation(libs.kotlinCompilerEmbeddable)
}

gradlePlugin {
    plugins.register("resources") {
        id = "resources"
        implementationClass = "com.aglushkov.resource.ResourcesPlugin"
    }
}
