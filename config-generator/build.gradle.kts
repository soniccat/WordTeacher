plugins {
    id("org.jetbrains.kotlin.jvm") version ("1.8.20")
//    id("publication-convention")
//    id("com.gradle.plugin-publish") version ("1.2.0")
    id("java-gradle-plugin")
}

group = "com.aglushkov.configs"
version = 0.1

dependencies {
    implementation(gradleKotlinDsl())
    compileOnly(libs.kotlinGradlePlugin)
//    compileOnly(libs.androidGradlePlugin)
//    compileOnly(libs.kotlinCompilerEmbeddable)
//    compileOnly(libs.androidSdkCommon)
    implementation(libs.kotlinPoet)
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain(11)
}

gradlePlugin {
    plugins {
        create("config-generator") {
            id = "com.aglushkov.config-generator"
            implementationClass = "com.aglushkov.gradle.ConfigGeneratorPlugin"

            displayName = "Config Generator"
            description = "Config class generator from properties files"
            tags.set(listOf("config", "properties", "kotlin"))
        }
    }

    website.set("todo")
    vcsUrl.set("todo")
}
